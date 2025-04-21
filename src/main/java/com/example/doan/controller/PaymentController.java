package com.example.doan.controller;

import com.example.doan.dto.request.ApiResponse;
import com.example.doan.dto.request.OrderRequest;
import com.example.doan.dto.response.OrderResponse;
import com.example.doan.entity.Order;
import com.example.doan.entity.PaymentLog;
import com.example.doan.service.MomoService;
import com.example.doan.service.OrderService;
import com.example.doan.service.PaymentService;
import com.example.doan.repository.PaymentLogRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final OrderService orderService;
    private final MomoService momoService;
    private final PaymentLogRepository paymentLogRepository;

    @PostMapping("/checkout")
    public ApiResponse<OrderResponse> checkout(@RequestBody OrderRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Order order = paymentService.handleCheckout(username, request);
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.convertToDto(order))
                .build();
    }

    // ✅ Tạo URL thanh toán online
    @PostMapping("/momo/create")
    public ApiResponse<String> createMomoPayment(@RequestParam Long orderId, @RequestParam Long amount) {
        try {
            String payUrl = momoService.createPaymentUrl(orderId.toString(), amount);
            return ApiResponse.<String>builder().result(payUrl).build();
        } catch (Exception e) {
            return ApiResponse.<String>builder().message("Failed to create payment URL: " + e.getMessage()).build();
        }
    }

    // ✅ Momo callback IPN
    @PostMapping("/momo/ipn")
    public String handleMomoIpn(@RequestBody Map<String, String> payload) {
        System.out.println("Momo IPN received: " + payload);

        String receivedSignature = payload.get("signature");

        String rawSignature = "accessKey=" + momoService.getAccessKey()
                + "&amount=" + payload.get("amount")
                + "&extraData=" + payload.get("extraData")
                + "&message=" + payload.get("message")
                + "&orderId=" + payload.get("orderId")
                + "&orderInfo=" + payload.get("orderInfo")
                + "&orderType=" + payload.get("orderType")
                + "&partnerCode=" + payload.get("partnerCode")
                + "&payType=" + payload.get("payType")
                + "&requestId=" + payload.get("requestId")
                + "&responseTime=" + payload.get("responseTime")
                + "&resultCode=" + payload.get("resultCode")
                + "&transId=" + payload.get("transId")
                + "&message=" + payload.get("message")
                + "&localMessage=" + payload.get("localMessage")
                + "&requestType=captureWallet";

        String mySignature = HmacUtils.hmacSha256Hex(momoService.getSecretKey(), rawSignature);

        if (!mySignature.equals(receivedSignature)) {
            return "INVALID SIGNATURE";
        }

        // ✅ Ghi log giao dịch
        PaymentLog log = PaymentLog.builder()
                .orderId(payload.get("orderId"))
                .amount(payload.get("amount"))
                .transId(payload.get("transId"))
                .resultCode(payload.get("resultCode"))
                .message(payload.get("message"))
                .time(Instant.now())
                .build();
        paymentLogRepository.save(log);

        if ("0".equals(payload.get("resultCode"))) {
            Long orderId = Long.parseLong(payload.get("orderId"));
            Order order = orderService.findById(orderId);
            order.setPaymentStatus("PAID");
            order.setStatus(Order.OrderStatus.CONFIRMED);
            orderService.save(order);
        }

        return "IPN received";
    }

    // ✅ Momo trả về người dùng sau khi thanh toán xong
    @GetMapping("/momo/return")
    public RedirectView momoReturn(@RequestParam Map<String, String> queryParams) {
        String resultCode = queryParams.get("resultCode");
        String redirectUrl;

        if ("0".equals(resultCode)) {
            redirectUrl = "http://localhost:5173/payment/success";
        } else {
            redirectUrl = "http://localhost:5173/payment/failure";
        }

        return new RedirectView(redirectUrl);
    }
}
