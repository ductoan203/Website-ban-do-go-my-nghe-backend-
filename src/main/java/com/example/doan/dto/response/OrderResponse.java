package com.example.doan.dto.response;

import com.example.doan.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private String shippingAddress;
    private String status;
    private BigDecimal total;
    private Instant createdAt;
    private List<OrderItemResponse> items;
    private String paymentMethod; // "COD" hoặc "ONLINE"
    private String paymentStatus;
    private String customerName;
    private String email;
    private String phone;
    private String cancelledBy;
    private UserResponse user;

    public static OrderResponse fromOrder(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .shippingAddress(order.getShippingAddress())
                .status(order.getStatus().name())
                .total(order.getTotal())
                .createdAt(order.getCreatedAt())
                .items(order.getItems().stream()
                        .map(OrderItemResponse::fromOrderItem)
                        .collect(Collectors.toList()))
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .customerName(order.getCustomerName())
                .email(order.getEmail())
                .phone(order.getPhone())
                .cancelledBy(order.getCancelledBy())
                .user(order.getUser() != null ? UserResponse.fromUser(order.getUser()) : null)
                .build();
    }
}