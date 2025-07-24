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
import java.util.Arrays;
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
        if (userRepository.count() == 0) {
            User adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setPassword(passwordEncoder.encode("password")); // 密码是 "password"
            adminUser.setRole("ADMIN");
            userRepository.save(adminUser);

            User regularUser = new User();
            regularUser.setUsername("user");
            regularUser.setPassword(passwordEncoder.encode("password"));
            regularUser.setRole("USER");
            userRepository.save(regularUser);

            Order order1 = new Order();
            order1.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            order1.setStatus(OrderStatus.PAID);
            order1.setAmount(new BigDecimal("1250.75"));
            order1.setCreationDate(LocalDateTime.now().minusDays(1));
            order1.setUser(adminUser);

            Order order2 = new Order();
            order2.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            order2.setStatus(OrderStatus.PENDING_PAYMENT);
            order2.setAmount(new BigDecimal("888.00"));
            order2.setCreationDate(LocalDateTime.now());
            order2.setUser(regularUser);

            orderRepository.saveAll(Arrays.asList(order1, order2));
        }
    }
}
