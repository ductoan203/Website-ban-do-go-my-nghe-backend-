package com.example.doan.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CartItemResponse {
    private Long productId;
    private String productName;
    private String thumbnailUrl;
    private BigDecimal price;
    private int quantity;
    private BigDecimal subtotal;
}