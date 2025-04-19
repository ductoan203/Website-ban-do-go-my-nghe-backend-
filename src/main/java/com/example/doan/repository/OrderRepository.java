package com.example.doan.repository;

import com.example.doan.entity.Order;
import com.example.doan.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
    List<Order> findByStatus(Order.OrderStatus status);
    List<Order> findByUserAndStatus(User user, Order.OrderStatus status);
}
