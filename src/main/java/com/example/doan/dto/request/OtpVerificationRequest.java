package com.example.doan.dto.request;

import lombok.Data;

@Data
public class OtpVerificationRequest {
    private String email;
    private String otpCode;
}
