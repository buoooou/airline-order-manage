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

    @Scheduled(fixedRate = 60000)
    @Transactional
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