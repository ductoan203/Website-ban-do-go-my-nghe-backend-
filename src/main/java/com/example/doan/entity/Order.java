package com.example.doan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    private String shippingAddress;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private BigDecimal total;

    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items;

    private String cancelledBy; // có thể là "USER", "ADMIN"

    public enum OrderStatus {
        PENDING, CONFIRMED, SHIPPED, CANCELLED
    }
}
