package com.example.doan.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {
    private String customerName;
    private String email;
    private String phone;
    private String shippingAddress;
    private String paymentMethod;
    private List<CartItemRequest> items;
}