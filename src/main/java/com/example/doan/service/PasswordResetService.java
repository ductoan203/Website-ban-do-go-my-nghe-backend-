package com.example.doan.service;

import com.example.doan.entity.PasswordResetToken;
import com.example.doan.entity.User;
import com.example.doan.exception.AppException;
import com.example.doan.exception.ErrorCode;
import com.example.doan.repository.PasswordResetTokenRepository;
import com.example.doan.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void sendResetToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Xoá token cũ nếu có
        tokenRepository.deleteAllByUser(user);

        // Tạo mã OTP 6 chữ số
        String token;
        do {
            token = String.valueOf(new Random().nextInt(900000) + 100000);
        } while (tokenRepository.findByToken(token).isPresent());

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiredAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .used(false)
                .build();

        tokenRepository.save(resetToken);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Yêu cầu đặt lại mật khẩu");
        message.setText("Mã OTP đặt lại mật khẩu của bạn là: " + token + "\nMã có hiệu lực trong 10 phút.");

        mailSender.send(message);
    }

    @Transactional
    public void resetPassword(String email, String token, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_OTP));

        if (resetToken.isUsed()) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        if (resetToken.getExpiredAt().isBefore(Instant.now())) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Reset mật khẩu cho email: {}, token: {}", email, token);
    }
}
