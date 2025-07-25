package com.postion.airlineorderbackend.service.impl;

import com.postion.airlineorderbackend.adapter.outbound.AirlineApiClient;
import com.postion.airlineorderbackend.dto.OrderDto;
import com.postion.airlineorderbackend.model.Order;
import com.postion.airlineorderbackend.model.OrderStatus;
import com.postion.airlineorderbackend.repository.OrderRepository;
import com.postion.airlineorderbackend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService { // <-- 实现了 OrderService 接口

    private final OrderRepository orderRepository;
    private final AirlineApiClient airlineApiService;
    @Override
    @Transactional
    public OrderDto payOrder(Long id) {
        Order order = findOrderById(id);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("只有待支付的订单才能支付。当前状态: " + order.getStatus());
        }
        order.setStatus(OrderStatus.PAID);
        // 异步触发下一步：出票
        requestTicketIssuance(order.getId());
        return convertToDto(orderRepository.save(order));
    }

    @Override
    @Async // (核心教学点A) 标记为异步方法，它将在独立的线程中执行
    @Transactional
    public void requestTicketIssuance(Long id) {
        Order order = findOrderById(id);
        // 再次检查状态，防止重复调用
        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.TICKETING_FAILED) {
            System.err.println("无法为订单 " + id + " 请求出票，因为状态不是PAID或TICKETING_FAILED。");
            return;
        }

        order.setStatus(OrderStatus.TICKETING_IN_PROGRESS);
        orderRepository.save(order);

        try {
            // (核心教学点B) 调用可能失败或超时的下游API
            String ticketNumber = airlineApiService.issueTicket(order.getId());
            // 成功后更新状态
            order.setStatus(OrderStatus.TICKETED);
            // 这里可以保存票号等信息
            // order.setTicketNumber(ticketNumber);
        } catch (Exception e) {
            // 失败后更新为失败状态
            System.err.println("订单 " + id + " 出票流程异常: " + e.getMessage());
            order.setStatus(OrderStatus.TICKETING_FAILED);
        }
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public OrderDto cancelOrder(Long id) {
        Order order = findOrderById(id);
        // 只有终态（已出票）的订单不能取消
        if (order.getStatus() == OrderStatus.TICKETED) {
            throw new IllegalStateException("已出票的订单不能取消。");
        }
        order.setStatus(OrderStatus.CANCELLED);
        return convertToDto(orderRepository.save(order));
    }

    // (核心教学点C) 定时任务，每分钟检查一次支付超时的订单
    @Scheduled(fixedRate = 60000) // 60 * 1000 ms = 1分钟
    @Transactional
    public void cancelUnpaidOrders() {
        System.out.println("【定时任务】开始检查支付超时订单...");
        // 查找创建超过15分钟且仍处于 PENDING_PAYMENT 状态的订单
        LocalDateTime fifteenMinutesAgo = LocalDateTime.now().minusMinutes(15);
        List<Order> unpaidOrders = orderRepository.findByStatusAndCreationDateBefore(
                OrderStatus.PENDING_PAYMENT,
                fifteenMinutesAgo
        );

        if (!unpaidOrders.isEmpty()) {
            System.out.println("发现 " + unpaidOrders.size() + " 个超时订单，将它们取消。");
            for (Order order : unpaidOrders) {
                order.setStatus(OrderStatus.CANCELLED);
            }
            orderRepository.saveAll(unpaidOrders);
        } else {
            System.out.println("【定时任务】未发现支付超时订单。");
        }
    }


    private Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
    }

    @Override // <-- 添加 @Override 注解
    public List<OrderDto> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override // <-- 添加 @Override 注解
    public OrderDto getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        OrderDto dto = convertToDto(order);

        // 模拟外部API调用，丰富DTO信息
        Map<String, Object> flightInfo = new HashMap<>();
        flightInfo.put("flightNumber", "MU5180");
        flightInfo.put("departure", "Shanghai");
        flightInfo.put("arrival", "Beijing");
        flightInfo.put("departureTime", "2024-10-27T14:30:00");

        dto.setFlightInfo(flightInfo);
        return dto;
    }

    // private 方法是实现细节，保持不变，它不属于接口的一部分
    private OrderDto convertToDto(Order order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setStatus(order.getStatus());
        dto.setAmount(order.getAmount());
        dto.setCreationDate(order.getCreationDate());

        OrderDto.UserDto userDto = new OrderDto.UserDto();
        userDto.setId(order.getUser().getId());
        userDto.setUsername(order.getUser().getUsername());
        dto.setUser(userDto);

        return dto;
    }
}