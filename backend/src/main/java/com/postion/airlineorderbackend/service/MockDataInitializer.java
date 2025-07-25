package com.postion.airlineorderbackend.service;

import com.postion.airlineorderbackend.model.Order;
import com.postion.airlineorderbackend.model.OrderStatus;
import com.postion.airlineorderbackend.model.User;
import com.postion.airlineorderbackend.repository.OrderRepository;
import com.postion.airlineorderbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MockDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 只有当数据库为空时才插入数据，防止重复
        if (userRepository.count() > 0 && orderRepository.count() > 0) {
            System.out.println("数据库已有数据，跳过初始化。");
            return;
        }

        System.out.println("数据库为空，开始插入Mock测试数据...");

        // 1. 创建用户
        User adminUser = createUser("admin", "password", "ADMIN");
        User regularUser = createUser("user", "password", "USER");

        // 2. 创建一个订单列表
        List<Order> ordersToCreate = new ArrayList<>();

        // --- 为 adminUser 创建订单 ---

        // 订单 1: 已支付 -> 用于测试异步出票流程 (支付后会自动触发)
        ordersToCreate.add(createOrder(
                OrderStatus.PAID,
                new BigDecimal("1250.75"),
                LocalDateTime.now().minusDays(1),
                adminUser,
                "测试案例: 已支付"
        ));

        // 订单 2: 已出票 (最终成功状态)
        ordersToCreate.add(createOrder(
                OrderStatus.TICKETED,
                new BigDecimal("3400.00"),
                LocalDateTime.now().minusDays(5),
                adminUser,
                "测试案例: 已出票"
        ));

        // 订单 3: 出票失败 -> 用于测试“重试出票”功能
        ordersToCreate.add(createOrder(
                OrderStatus.TICKETING_FAILED,
                new BigDecimal("980.50"),
                LocalDateTime.now().minusHours(2),
                adminUser,
                "测试案例: 出票失败"
        ));

        // 订单 4: 支付超时 -> 用于测试定时任务自动取消
        ordersToCreate.add(createOrder(
                OrderStatus.PENDING_PAYMENT,
                new BigDecimal("550.00"),
                LocalDateTime.now().minusMinutes(30), // 30分钟前创建，已超时
                adminUser,
                "测试案例: 支付超时"
        ));


        // --- 为 regularUser 创建订单 ---

        // 订单 5: 待支付 (正常) -> 用于测试“立即支付”功能
        ordersToCreate.add(createOrder(
                OrderStatus.PENDING_PAYMENT,
                new BigDecimal("888.00"),
                LocalDateTime.now().minusMinutes(5), // 5分钟前创建，未超时
                regularUser,
                "测试案例: 待支付 (正常)"
        ));

        // 订单 6: 已取消 (最终失败状态)
        ordersToCreate.add(createOrder(
                OrderStatus.CANCELLED,
                new BigDecimal("1100.20"),
                LocalDateTime.now().minusDays(2),
                regularUser,
                "测试案例: 已取消"
        ));

        // 订单 7: 出票中 -> 模拟一个中间状态，测试UI展示
        ordersToCreate.add(createOrder(
                OrderStatus.TICKETING_IN_PROGRESS,
                new BigDecimal("4321.00"),
                LocalDateTime.now().minusMinutes(10),
                regularUser,
                "测试案例: 出票中"
        ));

        // 批量保存所有实体
        orderRepository.saveAll(ordersToCreate);

        System.out.println("成功插入 " + userRepository.count() + " 个用户和 " + orderRepository.count() + " 条订单数据。");
    }

    /**
     * 创建并保存一个用户的辅助方法
     */
    private User createUser(String username, String rawPassword, String role) {
        // 检查用户是否已存在，避免重复创建
        return userRepository.findByUsername(username).orElseGet(() -> {
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setRole(role);
            return userRepository.save(user);
        });
    }

    /**
     * 创建一个订单对象的辅助方法 (不保存)
     */
    private Order createOrder(OrderStatus status, BigDecimal amount, LocalDateTime creationDate, User user, String description) {
        Order order = new Order();
        // 使用更具描述性的订单号
        order.setOrderNumber(status.name().substring(0, 3) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setStatus(status);
        order.setAmount(amount);
        order.setCreationDate(creationDate);
        order.setUser(user);

        // 注意：这里只是为了方便在日志中查看，并不会存到数据库
        // System.out.println("准备创建订单: " + order.getOrderNumber() + " | " + description);

        return order;
    }
}