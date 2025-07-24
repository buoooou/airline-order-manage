package com.postion.airlineorderbackend.repository;

import com.postion.airlineorderbackend.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}