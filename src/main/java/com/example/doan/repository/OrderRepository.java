package com.example.doan.repository;

import com.example.doan.entity.Order;
import com.example.doan.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);

    List<Order> findByStatus(Order.OrderStatus status);

    List<Order> findByUserAndStatus(User user, Order.OrderStatus status);

    Long countByStatus(Order.OrderStatus status);

    List<Order> findByStatusIn(List<Order.OrderStatus> statuses);

    List<Order> findByCreatedAtBetween(Instant start, Instant end);

    List<Order> findByCreatedAtBetweenAndStatus(Instant start, Instant end, Order.OrderStatus status);

    List<Order> findByCreatedAtBetweenAndStatusIn(Instant start, Instant end, List<Order.OrderStatus> statuses);

    Optional<Order> findById(Long id);

    long countByCreatedAtBetween(Instant start, Instant end);

    List<Order> findTop5ByOrderByCreatedAtDesc();

    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Order> findByStatusOrderByCreatedAtDesc(Order.OrderStatus status, Pageable pageable);

    List<Order> findByPaymentMethodAndStatusAndCreatedAtBefore(
            String paymentMethod,
            Order.OrderStatus status,
            Instant createdBefore);
}
