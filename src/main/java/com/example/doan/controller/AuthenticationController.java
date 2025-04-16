package com.example.doan.controller;


import com.example.doan.dto.request.*;
import com.example.doan.dto.response.AuthenticationResponse;
import com.example.doan.dto.response.IntrospectRespone;
import com.example.doan.entity.OtpToken;
import com.example.doan.entity.User;
import com.example.doan.exception.AppException;
import com.example.doan.exception.ErrorCode;
import com.example.doan.repository.OtpTokenRepository;
import com.example.doan.repository.UserRepository;
import com.example.doan.service.AuthenticatonService;

import com.example.doan.service.OtpService;
import com.example.doan.service.PasswordResetService;
import com.example.doan.service.UserService;
import com.nimbusds.jose.JOSEException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.time.Instant;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthenticationController {
    @Autowired
    AuthenticatonService authenticatonService;
    @Autowired
    private UserService userService;
    @Autowired
    private OtpTokenRepository otpTokenRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JavaMailSenderImpl mailSender;
    @Autowired
    private OtpService otpService;
    @Autowired
    private PasswordResetService passwordResetService;


    @PostMapping("/register")
    ApiResponse <User> createUser(@RequestBody @Valid UserCreateRequest request) {
        return ApiResponse.<User>builder()
                .result(userService.createUser(request))
                .build();
    }

    @PostMapping("/verify-otp")
    public ApiResponse<String> verifyOtp(@RequestBody OtpVerificationRequest request) {
        OtpToken token = otpTokenRepository.findByEmailAndOtpCode(request.getEmail(), request.getOtpCode())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_OTP));

        if (token.getExpiredAt().isBefore(Instant.now())) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        User user = token.getUser();
        user.setIsVerified(true);
        userRepository.save(user);

        otpTokenRepository.delete(token); // Xóa sau khi dùng

        return ApiResponse.<String>builder()
                .result("Xác minh thành công!")
                .build();
    }

    @GetMapping("/test-email")
    public String testEmail() {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("ninhductoan28112003@gmail.com");
        message.setSubject("Test gửi mail");
        message.setText("Nếu bạn nhận được email này, chức năng gửi mail đã hoạt động!");
        mailSender.send(message);
        return "Đã gửi!";
    }

    @PostMapping("/resend-otp")
    public ApiResponse<String> resendOtp(@RequestBody ResendOtpRequest request) {
        log.info("📩 Yêu cầu gửi lại OTP cho {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (Boolean.TRUE.equals(user.getIsVerified())) {
            throw new AppException(ErrorCode.USER_ALREADY_VERIFIED);
        }

        otpService.sendOtpToUser(user); // ❗ CHẮC CHẮN gọi được dòng này

        return ApiResponse.<String>builder()
                .result("Đã gửi lại OTP đến email của bạn.")
                .build();
    }

    @PostMapping("/login")
    ApiResponse<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        var authenticated = authenticatonService.authenticate(request);
        return ApiResponse.<AuthenticationResponse>builder()
                .result(authenticated)
                .build();
    }

    @PostMapping("/forgot-password")
    public ApiResponse<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        passwordResetService.sendResetToken(request.getEmail());
        return ApiResponse.<String>builder()
                .result("Đã gửi mã khôi phục đến email")
                .build();
    }

    @PostMapping("/reset-password")
    public ApiResponse<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(
                request.getEmail(),
                request.getOtp(),
                request.getNewPassword()
        );

        return ApiResponse.<String>builder()
                .result("Đặt lại mật khẩu thành công")
                .build();

    }


    @PostMapping("/introspect")
    ApiResponse<IntrospectRespone> authenticate(@RequestBody IntrospectRequest request)
            throws ParseException, JOSEException {
        var result = authenticatonService.introspect(request);
        return ApiResponse.<IntrospectRespone>builder()
                .result(result)
                .build();
    }
}