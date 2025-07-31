package com.postion.airlineorderbackend.controller;
import org.springframework.http.ResponseEntity;
import com.postion.airlineorderbackend.dto.OrderDto;
import com.postion.airlineorderbackend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.postion.airlineorderbackend.dto.ApiResponse;
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderDto>>> getAllOrders() {
        return ResponseEntity.ok(ApiResponse.success(orderService.getAllOrders()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDto>> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderById(id)));
    }
}