package com.example.doan.controller;

import com.example.doan.dto.request.ApiResponse;
import com.example.doan.dto.request.OrderRequest;
import com.example.doan.dto.response.OrderResponse;
import com.example.doan.entity.Order;
import com.example.doan.entity.PaymentLog;
import com.example.doan.entity.User;
import com.example.doan.service.*;
import com.example.doan.repository.PaymentLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.TreeMap;

import java.time.Instant;
import java.util.Map;

import static com.example.doan.service.VNPayService.hmacSHA512;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/payment")
public class PaymentController {
    @Autowired
    private final PaymentService paymentService;
    @Autowired
    private final OrderService orderService;
    @Autowired
    private final MomoService momoService;
    @Autowired
    private final PaymentLogRepository paymentLogRepository;
    @Autowired
    private final EmailService emailService;
    @Autowired
    private final VNPayService vnPayService;

    @PostMapping("/checkout")
    public ApiResponse<OrderResponse> checkout(@RequestBody OrderRequest request) {
        String username = null;

        try {
            username = SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception ignored) {
        }

        try {
            Order order = paymentService.handleCheckout(username, request);

            OrderResponse dto = orderService.convertToDto(order);
            System.out.println("🎯 DTO trả về: " + dto);
            return ApiResponse.<OrderResponse>builder()
                    .code(0)
                    .message("Thành công")
                    .result(dto)
                    .build();
        } catch (Exception e) {
            e.printStackTrace(); // <== quan trọng
            return ApiResponse.<OrderResponse>builder()
                    .code(9999)
                    .message("Lỗi khi đặt hàng: " + e.getMessage())
                    .build();
        }
    }

    // ✅ Tạo URL thanh toán online
    @PostMapping("/momo/create")
    public ApiResponse<String> createMomoPayment(@RequestParam Long orderId, @RequestParam Long amount) {
        try {
            String payUrl = momoService.createPaymentUrl(orderId.toString(), amount);
            System.out.println("✅ URL Momo trả về: " + payUrl);
            return ApiResponse.<String>builder().result(payUrl).build();
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.<String>builder().message("Failed to create payment URL: " + e.getMessage()).build();
        }
    }

    @GetMapping("/momo")
    public RedirectView payWithMomo(@RequestParam String orderId, @RequestParam Long amount) throws Exception {
        String payUrl = momoService.createPaymentUrl(orderId, amount);
        return new RedirectView(payUrl);
    }

    // ✅ Momo callback IPN
    @PostMapping("/momo/ipn")
    public String handleMomoIpn(@RequestBody Map<String, String> payload) {
        System.out.println("📥 Momo IPN received: " + payload); // Đảm bảo log này in ra

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
            orderService.deductStock(order);

            // Gửi email xác nhận sau khi thanh toán Momo thành công
            emailService.send(
                    order.getEmail(),
                    "Xác nhận đơn hàng #" + order.getId(),
                    "Cảm ơn bạn đã thanh toán qua Momo! Đơn hàng #" + order.getId()
                            + " của bạn đã được xác nhận và sẽ sớm được xử lý." // Nội dung email đơn giản
            );

        }

        return "IPN received";
    }

    // ✅ Momo trả về người dùng sau khi thanh toán xong
    @GetMapping("/momo/return")
    public RedirectView momoReturn(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String resultCode) {

        String redirectUrl = "http://localhost:5173/order-confirmation";
        String status = "failed";

        if ("0".equals(resultCode)) {
            status = "success";
            try {
                // Nếu orderId là số, parse luôn, nếu có dạng 177-xxx thì tách lấy số
                Long realOrderId = null;
                if (orderId != null && orderId.contains("-")) {
                    realOrderId = Long.parseLong(orderId.split("-")[0]);
                } else if (orderId != null) {
                    realOrderId = Long.parseLong(orderId);
                }
                if (realOrderId != null) {
                    Order order = orderService.findById(realOrderId);
                    order.setPaymentStatus("PAID");
                    order.setStatus(Order.OrderStatus.CONFIRMED);
                    orderService.save(order);
                    orderService.deductStock(order);
                    // Gửi email xác nhận sau khi thanh toán Momo thành công
                    emailService.send(
                            order.getEmail(),
                            "Xác nhận đơn hàng #" + order.getId(),
                            "Cảm ơn bạn đã thanh toán qua Momo! Đơn hàng #" + order.getId()
                                    + " của bạn đã được xác nhận và sẽ sớm được xử lý.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        String finalRedirectUrl = redirectUrl + "?orderId=" + (orderId != null ? orderId : "null") + "&status="
                + status;
        System.out.println("➡️ Chuyển hướng đến frontend: " + finalRedirectUrl);

        return new RedirectView(finalRedirectUrl);
    }

    @GetMapping("/vnpay/create")
    public ResponseEntity<?> createVnpayUrl(HttpServletRequest request,
                                            @RequestParam("amount") Long amount,
                                            @RequestParam("orderId") Long orderId) {
        try {
            String url = vnPayService.createPaymentUrl(request, amount, orderId.toString());
            return ResponseEntity.ok(url);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi tạo URL VNPAY");
        }
    }

    @PostMapping("/vnpay/ipn")
    public ResponseEntity<String> handleVnpayIpn(@RequestParam Map<String, String> params) {
        try {
            log.info("📥 [IPN] Nhận thông báo VNPAY: {}", params);

            // Bước 1: Lấy SecureHash từ request
            String receivedHash = params.get("vnp_SecureHash");

            // Bước 2: Xây dựng dữ liệu để hash lại
            Map<String, String> sorted = new TreeMap<>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!entry.getKey().equals("vnp_SecureHash") && !entry.getKey().equals("vnp_SecureHashType")) {
                    sorted.put(entry.getKey(), entry.getValue());
                }
            }

            StringBuilder hashData = new StringBuilder();
            for (Map.Entry<String, String> entry : sorted.entrySet()) {
                hashData.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
            hashData.setLength(hashData.length() - 1);

            String myHash = HmacUtils.hmacSha512Hex(vnPayService.getHashSecret(), hashData.toString());

            if (!myHash.equalsIgnoreCase(receivedHash)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("INVALID_SIGNATURE");
            }

            // ✅ Đúng chữ ký => xử lý đơn hàng
            Long orderId = Long.parseLong(params.get("vnp_TxnRef"));
            Order order = orderService.findById(orderId);
            order.setPaymentStatus("PAID");
            order.setStatus(Order.OrderStatus.CONFIRMED);
            orderService.save(order);
            orderService.deductStock(order);

            return ResponseEntity.ok("IPN_RECEIVED");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("ERROR");
        }
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<String> handleVnpayReturn(@RequestParam Map<String, String> allParams) {
        System.out.println("📥 VNPAY RETURN: " + allParams);

        String receivedHash = allParams.get("vnp_SecureHash");

        Map<String, String> sortedParams = new TreeMap<>();
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            if (!entry.getKey().equals("vnp_SecureHash") && !entry.getKey().equals("vnp_SecureHashType")) {
                sortedParams.put(entry.getKey(), entry.getValue());
            }
        }

        StringBuilder hashData = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            hashData.append(entry.getKey()).append('=').append(entry.getValue()).append('&');
        }
        if (hashData.length() > 0) {
            hashData.setLength(hashData.length() - 1);
        }

        String myHash;
        try {
            myHash = VNPayService.hmacSHA512(vnPayService.getHashSecret(), hashData.toString());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi tạo chữ ký: " + e.getMessage());
        }

        System.out.println("✅ So sánh chữ ký:");
        System.out.println("Generated: " + myHash);
        System.out.println("Received : " + receivedHash);

        if (!myHash.equalsIgnoreCase(receivedHash)) {
            return ResponseEntity.badRequest().body("❌ Sai chữ ký xác thực từ VNPAY");
        }

        try {
            Long orderId = Long.parseLong(allParams.get("vnp_TxnRef"));
            Order order = orderService.findById(orderId);

            order.setPaymentStatus("PAID");
            order.setStatus(Order.OrderStatus.CONFIRMED);
            orderService.save(order);
            orderService.deductStock(order);

            emailService.send(
                    order.getEmail(),
                    "Xác nhận đơn hàng #" + order.getId(),
                    "Cảm ơn bạn đã thanh toán qua VNPAY. Đơn hàng của bạn đã được xác nhận với tổng tiền là "
                            + order.getTotal() + " VNĐ.");

            String html = "<html><head>"
                    + "<meta http-equiv='refresh' content='5; url=http://localhost:5173' />"
                    + "<style>body{font-family:sans-serif;text-align:center;margin-top:100px;}</style>"
                    + "</head><body>"
                    + "<h2 style='color:green'>🎉 Thanh toán VNPAY thành công!</h2>"
                    + "<p>Đơn hàng đã được ghi nhận.</p>"
                    + "<p>Bạn sẽ được chuyển về trang chủ sau vài giây...</p>"
                    + "</body></html>";

            return ResponseEntity.ok().body(html);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ Lỗi xử lý đơn hàng sau thanh toán");
        }
    }

}
