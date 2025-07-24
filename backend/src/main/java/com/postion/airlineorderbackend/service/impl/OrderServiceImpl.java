package com.postion.airlineorderbackend.service.impl;

import com.postion.airlineorderbackend.dto.OrderDto;
import com.postion.airlineorderbackend.model.Order;
import com.postion.airlineorderbackend.repository.OrderRepository;
import com.postion.airlineorderbackend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService { // <-- 实现了 OrderService 接口

    private final OrderRepository orderRepository;

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