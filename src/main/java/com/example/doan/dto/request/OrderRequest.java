package com.example.doan.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderRequest {
    private String customerName;
    private String email;
    private String phone;
    private String shippingAddress;
    private String paymentMethod;
    private String paymentStatus;
    private List<CartItemRequest> items;
    private BigDecimal total;
}