package com.postion.airlineorderbackend.service;

import com.postion.airlineorderbackend.dto.OrderDto;
import com.postion.airlineorderbackend.model.Order;
import com.postion.airlineorderbackend.model.OrderStatus;
import com.postion.airlineorderbackend.model.User;
import com.postion.airlineorderbackend.repository.OrderRepository;
import com.postion.airlineorderbackend.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // 启用 Mockito 注解支持
class OrderServiceImplTest {

    @Mock // 创建一个 OrderRepository 的模拟对象
    private OrderRepository orderRepository;

    @InjectMocks // 创建 OrderServiceImpl 实例，并自动注入上面 @Mock 标记的模拟对象
    private OrderServiceImpl orderService;

    private User testUser;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        // 在每个测试运行前，准备好测试数据
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testOrder = new Order();
        testOrder.setId(100L);
        testOrder.setOrderNumber("ORD12345");
        testOrder.setStatus(OrderStatus.PAID);
        testOrder.setAmount(new BigDecimal("250.75"));
        testOrder.setCreationDate(LocalDateTime.now());
        testOrder.setUser(testUser);
    }

    @Test
    @DisplayName("当调用 getAllOrders 时，应返回所有订单的 DTO 列表")
    void shouldReturnAllOrdersAsDtoList() {
        // Arrange (准备)
        // 定义当 orderRepository.findAll() 被调用时，返回包含我们测试订单的列表
        when(orderRepository.findAll()).thenReturn(Collections.singletonList(testOrder));

        // Act (执行)
        List<OrderDto> result = orderService.getAllOrders();

        // Assert (断言)
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ORD12345", result.get(0).getOrderNumber());
        assertEquals("testuser", result.get(0).getUser().getUsername());

        // 验证 orderRepository.findAll() 方法确实被调用了1次
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("当使用有效 ID 调用 getOrderById 时，应返回对应的订单 DTO 并包含航班信息")
    void shouldReturnOrderDtoWithFlightInfoForValidId() {
        // Arrange
        // 定义当 orderRepository.findById() 使用特定ID被调用时，返回包含我们测试订单的 Optional
        when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));

        // Act
        OrderDto result = orderService.getOrderById(100L);

        // Assert
        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("ORD12345", result.getOrderNumber());
        assertNotNull(result.getFlightInfo(), "航班信息不应为空");
        assertEquals("MU5180", result.getFlightInfo().get("flightNumber"));

        // 验证 orderRepository.findById() 方法确实被调用了1次
        verify(orderRepository, times(1)).findById(100L);
    }

    @Test
    @DisplayName("当使用无效 ID 调用 getOrderById 时，应抛出 RuntimeException")
    void shouldThrowExceptionForInvalidId() {
        // Arrange
        long invalidId = 999L;
        // 定义当 orderRepository.findById() 使用无效ID被调用时，返回一个空的 Optional
        when(orderRepository.findById(invalidId)).thenReturn(Optional.empty());

        // Act & Assert
        // 断言当执行 getOrderById 时，会抛出 RuntimeException
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.getOrderById(invalidId);
        });

        assertEquals("Order not found", exception.getMessage());

        // 验证 orderRepository.findById() 方法确实被调用了1次
        verify(orderRepository, times(1)).findById(invalidId);
    }
}