package com.example.doan.repository;

import com.example.doan.entity.Order;
import com.example.doan.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
    List<Order> findByStatus(Order.OrderStatus status);
    List<Order> findByUserAndStatus(User user, Order.OrderStatus status);
    Long countByStatus(Order.OrderStatus status);
    List<Order> findByStatusIn(List<Order.OrderStatus> statuses);
    List<Order> findByCreatedAtBetween(Instant start, Instant end);

    Optional<Order> findById(Long id);
}
