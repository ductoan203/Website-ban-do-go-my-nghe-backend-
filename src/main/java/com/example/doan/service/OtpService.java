package com.example.doan.service;

import com.example.doan.entity.OtpToken;
import com.example.doan.entity.User;
import com.example.doan.repository.OtpTokenRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

@RequiredArgsConstructor
@Service
@Slf4j
public class OtpService {
    @Autowired
    private JavaMailSender mailSender;
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
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject("Xác thực tài khoản - Đồ Gỗ Mỹ Nghệ Hùng Dũng");
            message.setText("Mã OTP mới của bạn là: " + otpCode +
                    "\nMã có hiệu lực trong 5 phút.");
            mailSender.send(message);
            log.info("✅ Đã gửi lại OTP {} cho {}", otpCode, user.getEmail());
        } catch (Exception e) {
            log.error("❌ Gửi OTP thất bại: {}", e.getMessage(), e);
        }
        log.info("✅ Gửi xong OTP: {}", otpCode);

    }

}
