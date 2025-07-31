package com.postion.airlineorderbackend.service.impl;

import com.postion.airlineorderbackend.adapter.outbound.AirlineApiClient;
import com.postion.airlineorderbackend.dto.OrderDto;
import com.postion.airlineorderbackend.exception.BusinessException;
import com.postion.airlineorderbackend.mapper.OrderMapper;
import com.postion.airlineorderbackend.model.Order;
import com.postion.airlineorderbackend.model.OrderStatus;
import com.postion.airlineorderbackend.repository.OrderRepository;
import com.postion.airlineorderbackend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final AirlineApiClient airlineApiService;
    private final OrderMapper orderMapper;
    @Override
    @Transactional
    public OrderDto payOrder(Long id) {
        log.info("开始处理支付订单请求，订单ID: {}", id);
        Order order = findOrderById(id);

        // 状态机校验：只有 PENDING_PAYMENT 状态的订单才能支付
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            log.warn("支付失败：订单 {} 状态不是 PENDING_PAYMENT，当前状态为 {}", id, order.getStatus());
            throw new BusinessException(HttpStatus.BAD_REQUEST, "只有待支付的订单才能支付。当前状态: " + order.getStatus());
        }

        order.setStatus(OrderStatus.PAID);
        Order savedOrder = orderRepository.save(order);
        log.info("订单 {} 状态已更新为 PAID", id);

        // 异步触发下一步：出票
        requestTicketIssuance(order.getId());
        return orderMapper.toDto(savedOrder);
    }

    @Override
    @Async("taskExecutor") // 建议指定一个线程池
    @Transactional
    public void requestTicketIssuance(Long id) {
        log.info("异步任务启动：为订单 {} 请求出票", id);
        Order order = findOrderById(id);

        // 状态机校验：只有 PAID 或 TICKETING_FAILED 状态的订单才能（重新）请求出票
        List<OrderStatus> validStates = Arrays.asList(OrderStatus.PAID, OrderStatus.TICKETING_FAILED);
        if (!validStates.contains(order.getStatus())) {
            log.warn("无法为订单 {} 请求出票，因为其状态 ({}) 不是 PAID 或 TICKETING_FAILED", id, order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.TICKETING_IN_PROGRESS);
        orderRepository.save(order);
        log.info("订单 {} 状态更新为 TICKETING_IN_PROGRESS", id);

        try {
            // 调用可能失败或超时的下游API
            String ticketNumber = airlineApiService.issueTicket(order.getId());
            order.setStatus(OrderStatus.TICKETED);
            log.info("订单 {} 出票成功！票号: {}", id, ticketNumber);
            // 这里可以保存票号等信息
            // order.setTicketNumber(ticketNumber);
        } catch (Exception e) {
            order.setStatus(OrderStatus.TICKETING_FAILED);
            log.error("订单 {} 出票流程异常，状态更新为 TICKETING_FAILED。错误: {}", id, e.getMessage());
        }
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public OrderDto cancelOrder(Long id) {
        log.info("开始处理取消订单请求，订单ID: {}", id);
        Order order = findOrderById(id);

        // 状态机校验：已出票和已取消的订单不能再次取消
        List<OrderStatus> finalStates = Arrays.asList(OrderStatus.TICKETED, OrderStatus.CANCELLED);
        if (finalStates.contains(order.getStatus())) {
            log.warn("取消失败：订单 {} 已处于终态 ({})，无法取消", id, order.getStatus());
            throw new BusinessException(HttpStatus.BAD_REQUEST, "此状态的订单无法取消: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);
        log.info("订单 {} 已被成功取消", id);
        return orderMapper.toDto(savedOrder);
    }

    /***
     *
     * 1。重复执行 (Duplicate Execution)
     * 这是最核心的问题。假设您部署了3个节点，那么在同一时间点（例如，每分钟的第0秒），所有3个节点都会同时触发 cancelUnpaidOrders 这个定时任务。
     * 2。资源浪费 (Resource Waste)
     * 每个节点都会去数据库执行一次查询：findByStatusAndCreationDateBefore(...)。如果超时订单有100个，那么数据库在同一瞬间会收到3次几乎一模一样的查询请求，浪费了数据库的I/O和CPU资源。
     * 3。数据竞争与业务逻辑错误 (Data Race & Logic Errors)
     * 这是最危险的一点。设想一下执行流程：
     * 时刻 T0: 节点A、B、C 同时查到10个待取消的订单（ID 1-10）。
     * 时刻 T1: 节点A 执行 saveAll，将这10个订单的状态更新为 CANCELLED。
     * 时刻 T2: 节点B 也执行 saveAll，它会再次尝试将这10个订单的状态更新为 CANCELLED。虽然对于这个简单的状态更新来说，结果可能还是一样的，但这是无效的重复操作。
     * 4。更坏的情况: 如果您的业务逻辑更复杂，比如“取消订单后要给用户发送一封邮件通知”，那么用户就会收到3封一模一样的取消邮件，这是非常糟糕的用户体验。如果涉及到退款操作，则可能导致重复退款，造成资金损失。
     * 日志混乱与排查困难 (Logging Chaos)
     * 您的日志系统里会同时出现来自3个不同节点的日志，它们都声称自己“发现了10个超时订单”并“将它们取消了”。当出现问题时，你很难追踪到底是哪个节点真正完成了工作，也无法判断是否发生了重复操作。
     *
     *方案一：基于数据库的实现
     * 实现方式1：数据库悲观锁 (Pessimistic Locking)
     * 原理： 在执行任务前，先尝试去锁住一个“任务锁”记录。例如，创建一个 task_locks 表，里面有一条记录 task_name = 'cancelUnpaidOrdersTask'。
     * 流程：
     * 开启事务。
     * 执行 SELECT * FROM task_locks WHERE task_name = 'cancelUnpaidOrdersTask' FOR UPDATE。
     * FOR UPDATE 会对这一行加上排它锁。第一个成功执行该语句的节点会获得锁，其他节点会被阻塞，直到锁被释放。
     * 获得锁的节点执行业务逻辑。
     * 提交事务，释放锁。
     * 优点： 实现简单，无需引入新的中间件。
     * 缺点： 性能开销大，会对数据库造成压力。如果获得锁的节点崩溃，锁不会被释放，可能造成死锁（需要配合超时机制）。
     *
     * 实现方式2：数据库乐观锁 (Optimistic Locking)
     * 原理： 在 task_locks 表中增加一个 version 字段或 timestamp 字段。
     * 流程：
     * 节点A读取锁记录（包含 version=1）。
     * 节点A执行业务逻辑。
     * 节点A更新锁记录：UPDATE task_locks SET version = version + 1 WHERE task_name = '...' AND version = 1。
     * 如果此时有其他节点B也想获取锁并更新，它的 UPDATE 语句会因为 version 不匹配而失败（更新0行）。失败的节点就知道自己没抢到锁，直接放弃本次任务。
     * 优点： 比悲观锁性能好，不会产生阻塞。
     * 缺点： 实现相对复杂，需要自己处理“更新失败”（即没抢到锁）的情况。
     *
     *
     *方案二：基于 Redis 的实现
     * 利用 Redis 高性能的原子操作来实现分布式锁。
     * 实现方式：SETNX (SET if Not eXists)
     * 原理： SET lock_key random_value NX PX timeout 是一个原子命令。
     * lock_key: 锁的唯一标识，例如 lock:cancelUnpaidOrdersTask。
     * random_value: 一个随机值，用于安全地释放锁（防止误删其他节点持有的锁）。
     * NX: 只在 lock_key 不存在时才设置成功，返回 OK。如果已存在，则设置失败。
     * PX timeout: 设置一个过期时间（例如60秒）。这是为了防止获得锁的节点宕机而无法释放锁，导致死锁。
     * 流程：
     * 所有节点都尝试执行 SETNX 命令。
     * 只有一个节点会成功，它就获得了锁。
     * 执行业务逻辑。
     * 业务执行完毕后，通过 DEL 命令删除该 key 来释放锁（释放前要校验 random_value 是否是自己设置的，防止误删）。
     * 优点： 性能极高，实现简单，是业界最主流的分布式锁方案之一。
     * 缺点： 需要引入并维护一个 Redis 集群。自己实现完美的 Redis 锁有一定复杂度（例如锁的可重入、自动续期等），通常会使用 Redisson 这样的成熟客户端库。
     *
     *方案三：基于 ZooKeeper 的实现
     * 利用 ZooKeeper 的特性来实现分布式锁。
     * 原理：临时有序节点 (Ephemeral Sequential Znode)
     * 流程：
     * 在一个持久节点（例如 /locks）下，所有希望获取锁的节点都创建一个临时有序节点（例如 /locks/task-00000001、/locks/task-00000002）。
     * 每个节点创建完自己的节点后，获取 /locks 下的所有子节点，并判断自己创建的节点序号是否是最小的。
     * 如果是最小的，则该节点获得锁。
     * 如果不是，则该节点监听比它序号小的前一个节点。
     * 当任务执行完毕，获得锁的节点删除自己的临时节点（或因会话断开而自动删除）。它后面的节点会收到通知，并重新检查自己是否序号最小，从而实现锁的公平交接。
     * 优点： 可靠性非常高，没有死锁问题（临时节点会因客户端宕机而自动删除），能实现公平锁。
     * 缺点： 性能不如 Redis，且需要引入和维护一个更复杂的 ZooKeeper 集群。
     *
     *方案四：使用成熟的分布式任务调度框架
     * 当定时任务变得非常多且复杂时，最好的选择是引入一个专业的分布式任务调度平台。
     * 代表框架：XXL-Job、PowerJob、Elastic-Job
     * 原理： 这些框架通常包含一个调度中心（Admin）和一个执行器（Executor）。
     * 调度中心： 负责任务的管理、配置、调度和监控。它知道所有在线的执行器节点。
     * 执行器： 部署在您的业务应用中，负责接收调度中心的指令并执行具体的业务代码（JobHandler）。
     * 流程：
     * 您在调度中心的Web界面上配置一个任务（Cron表达式、路由策略、失败策略等）。
     * 到达触发时间时，调度中心根据您配置的路由策略（例如“第一个”、“轮询”、“分片广播”），向一个或多个执行器发送执行指令。
     * 执行器执行业务代码，并将结果（成功/失败/日志）回报给调度中心。
     * 优点： 功能极其强大，提供可视化管理、失败告警、任务分片、高可用、日志追溯等全套解决方案，将调度逻辑与业务逻辑完全解耦。
     * 缺点： 架构变重，需要额外部署和维护一个调度中心。
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    @SchedulerLock(
            name = "cancelUnpaidOrdersTask", // Must be unique for each task
            lockAtMostFor = "55s",           // The lock is released after 55s at most
            lockAtLeastFor = "10s"           // The lock is held for at least 10s
    )
    public void cancelUnpaidOrders() {
        log.info("【定时任务】开始检查并取消支付超时的订单...");
        LocalDateTime fifteenMinutesAgo = LocalDateTime.now().minusMinutes(15);
        List<Order> unpaidOrders = orderRepository.findByStatusAndCreationDateBefore(
                OrderStatus.PENDING_PAYMENT,
                fifteenMinutesAgo
        );

        if (!unpaidOrders.isEmpty()) {
            log.info("【定时任务】发现 {} 个超时订单，将它们的状态更新为 CANCELLED", unpaidOrders.size());
            for (Order order : unpaidOrders) {
                order.setStatus(OrderStatus.CANCELLED);
                log.debug("  - 订单 {} (创建于 {}) 已被标记为取消", order.getId(), order.getCreationDate());
            }
            orderRepository.saveAll(unpaidOrders);
        } else {
            log.info("【定时任务】未发现支付超时的订单。");
        }
    }

    private Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "未找到ID为 " + id + " 的订单"));
    }

    // 其他方法保持不变...
    @Override
    public List<OrderDto> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public OrderDto getOrderById(Long id) {
        Order order = findOrderById(id); // 复用 findOrderById 方法
        OrderDto dto = orderMapper.toDto(order);

        // 模拟外部API调用
        Map<String, Object> flightInfo = new HashMap<>();
        flightInfo.put("flightNumber", "MU5180");
        flightInfo.put("departure", "Shanghai");
        flightInfo.put("arrival", "Beijing");
        flightInfo.put("departureTime", "2024-10-27T14:30:00");
        dto.setFlightInfo(flightInfo);

        return dto;
    }

}