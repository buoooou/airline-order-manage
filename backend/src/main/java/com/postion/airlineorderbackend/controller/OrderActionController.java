package com.postion.airlineorderbackend.controller;


import com.postion.airlineorderbackend.dto.OrderDto;
import com.postion.airlineorderbackend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/orders/{id}")
@RequiredArgsConstructor
public class OrderActionController {

    private final OrderService orderService;

    @PostMapping("/pay")
    public ResponseEntity<OrderDto> pay(@PathVariable Long id) {
        OrderDto updatedOrder = orderService.payOrder(id);
        return ResponseEntity.ok(updatedOrder);
    }

    @PostMapping("/cancel")
    public ResponseEntity<OrderDto> cancel(@PathVariable Long id) {
        OrderDto updatedOrder = orderService.cancelOrder(id);
        return ResponseEntity.ok(updatedOrder);
    }

    @PostMapping("/retry-ticketing")
    public ResponseEntity<Void> retryTicketing(@PathVariable Long id) {
        orderService.requestTicketIssuance(id);
        // 对于异步任务，立即返回 Accepted 状态是最佳实践
        return ResponseEntity.accepted().body(null);
    }
}
