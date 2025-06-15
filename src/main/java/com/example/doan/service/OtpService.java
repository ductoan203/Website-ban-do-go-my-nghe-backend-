package com.example.doan.service;

import com.example.doan.entity.OtpToken;
import com.example.doan.entity.User;
import com.example.doan.repository.OtpTokenRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

@RequiredArgsConstructor
@Service
@Slf4j
public class OtpService {
    @Autowired
    private EmailService emailService;
    @Autowired
    private OtpTokenRepository otpTokenRepository;

    @Transactional
    public void sendOtpToUser(User user) {
        log.info("🧹 Xoá OTP cũ nếu có của user {}", user.getUserId());

        otpTokenRepository.deleteByUser(user); // <-- Custom JPQL này mới chạy thật

        log.info("✅ Đã xoá, tạo OTP mới");

        // Tạo mã OTP mới
        String otpCode = String.valueOf(new Random().nextInt(900000) + 100000);

        OtpToken otp = OtpToken.builder()
                .email(user.getEmail())
                .otpCode(otpCode)
                .expiredAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                .user(user)
                .build();

        otpTokenRepository.save(otp);

        // Gửi email
        try {
            emailService.sendOtpEmail(user.getEmail(), otpCode);
            log.info("✅ Đã gửi lại OTP {} cho {}", otpCode, user.getEmail());
        } catch (Exception e) {
            log.error("❌ Gửi OTP thất bại: {}", e.getMessage(), e);
        }
        log.info("✅ Gửi xong OTP: {}", otpCode);

    }

}
