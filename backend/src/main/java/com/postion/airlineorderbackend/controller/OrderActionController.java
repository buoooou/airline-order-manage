package com.postion.airlineorderbackend.controller;


import com.postion.airlineorderbackend.dto.OrderDto;
import com.postion.airlineorderbackend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.postion.airlineorderbackend.dto.ApiResponse;
@RestController
@RequestMapping("/api/orders/{id}")
@RequiredArgsConstructor
public class OrderActionController {

    private final OrderService orderService;

    @PostMapping("/pay")
    public ResponseEntity<ApiResponse<OrderDto>> pay(@PathVariable Long id) {
        OrderDto updatedOrder = orderService.payOrder(id);
        return ResponseEntity.ok(ApiResponse.success(updatedOrder));
    }

    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<OrderDto>> cancel(@PathVariable Long id) {
        OrderDto updatedOrder = orderService.cancelOrder(id);
        return ResponseEntity.ok(ApiResponse.success(updatedOrder));
    }

    @PostMapping("/retry-ticketing")
    public ResponseEntity<ApiResponse<Void>> retryTicketing(@PathVariable Long id) {
        orderService.requestTicketIssuance(id);
        // 对于异步任务，立即返回 Accepted 状态是最佳实践
        return ResponseEntity.accepted().body(ApiResponse.success(null));
    }
}
