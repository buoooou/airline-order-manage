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
        try {
            return ResponseEntity.ok(orderService.payOrder(id));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(null); // Or return a proper error DTO
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<OrderDto> cancel(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(orderService.cancelOrder(id));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/retry-ticketing")
    public ResponseEntity<Void> retryTicketing(@PathVariable Long id) {
        // 直接调用异步方法，立即返回202 Accepted
        orderService.requestTicketIssuance(id);
        return ResponseEntity.accepted().build();
    }
}
