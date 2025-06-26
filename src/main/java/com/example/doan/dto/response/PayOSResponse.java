package com.example.doan.dto.response;

import lombok.Data;

@Data
public class PayOSResponse {
    private int code;
    private String message;
    private String checkoutUrl;
}