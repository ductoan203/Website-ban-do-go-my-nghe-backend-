package com.example.doan.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import java.math.BigDecimal;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    public void sendEmail(String to, String subject, String content) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("noreply@hungdung.com"); // Hoặc email của bạn
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true); // true cho phép HTML

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Không thể gửi email", e);
        }
    }

    public void sendContactReply(String toEmail, String recipientName, String replyContent) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");

            Context context = new Context();
            context.setVariable("recipientName", recipientName);
            context.setVariable("replyContent", replyContent);

            String htmlContent = templateEngine.process("contact-reply", context);

            helper.setFrom("noreply@hungdung.com"); // Email của bạn
            helper.setTo(toEmail);
            helper.setSubject("Phản hồi từ bộ phận Hỗ trợ Hung Dung Shop");
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Không thể gửi email phản hồi", e);
        }
    }

    public void sendOrderConfirmationEmail(String toEmail, String customerName, String orderId, BigDecimal totalAmount,
                                           String paymentMethod, String shippingAddress) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");

            Context context = new Context();
            context.setVariable("customerName", customerName);
            context.setVariable("orderId", orderId);
            context.setVariable("totalAmount", totalAmount);
            context.setVariable("paymentMethod", paymentMethod);
            context.setVariable("shippingAddress", shippingAddress);

            // Thêm thông điệp thanh toán động
            String paymentMessage;
            if ("PAYOS".equalsIgnoreCase(paymentMethod) || "ONLINE".equalsIgnoreCase(paymentMethod)) {
                paymentMessage = "Bạn đã thanh toán thành công đơn hàng này.";
            } else {
                paymentMessage = "Bạn sẽ thanh toán khi nhận hàng.";
            }
            context.setVariable("paymentMessage", paymentMessage);

            String htmlContent = templateEngine.process("order-confirmation", context);

            helper.setFrom("noreply@hungdung.com"); // Email của bạn
            helper.setTo(toEmail);
            helper.setSubject("Xác nhận đơn hàng #" + orderId);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Không thể gửi email xác nhận đơn hàng", e);
        }
    }

    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");

            Context context = new Context();
            context.setVariable("otpCode", otpCode);

            String htmlContent = templateEngine.process("otp-email", context);

            helper.setFrom("noreply@hungdung.com");
            helper.setTo(toEmail);
            helper.setSubject("Xác thực tài khoản - Mã OTP của bạn");
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Không thể gửi email OTP", e);
        }
    }
}
