package com.postion.airlineorderbackend.model;

public enum OrderStatus {
    PENDING_PAYMENT,       // 待支付
    PAID,                  // 已支付
    TICKETING_IN_PROGRESS, // 出票中
    TICKETING_FAILED,      // 出票失败
    TICKETED,              // 已出票 (完成)
    CANCELLED              // 已取消
}