package com.example.doan.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private Long id;
    private String shippingAddress;
    private String status;
    private BigDecimal total;
    private Instant createdAt;
    private List<OrderItemResponse> items;
}