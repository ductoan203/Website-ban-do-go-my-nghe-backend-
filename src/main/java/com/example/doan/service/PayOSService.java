package com.example.doan.service;

import com.example.doan.config.PayOSConfig;
import com.example.doan.dto.request.PayOSCreateRequest;
import com.example.doan.dto.request.PayOSWebhookRequest;
import com.example.doan.dto.response.PayOSResponse;
import com.example.doan.entity.Order;
import com.example.doan.exception.AppException;
import com.example.doan.exception.ErrorCode;
import com.example.doan.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;
import vn.payos.type.WebhookData;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayOSService {
    private final PayOSConfig payOSConfig;
    private final OrderRepository orderRepository;

    @Autowired
    public PayOS payOS;

    public String createPaymentLink(PayOSCreateRequest request) throws Exception {
        try {
            log.info("Creating PayOS payment link for order: {}, amount: {}",
                    request.getOrderCode(), request.getAmount());

            // Tạo returnUrl/cancelUrl động theo orderId
            String returnUrl = "http://localhost:5173/order-confirmation?status=success&orderId="
                    + request.getOrderCode();
            String cancelUrl = "http://localhost:5173/order-confirmation?status=failed&orderId="
                    + request.getOrderCode();

            // Tạo danh sách các item từ request
            List<ItemData> items = new ArrayList<>();
            if (request.getItems() != null) {
                for (PayOSCreateRequest.Item item : request.getItems()) {
                    items.add(ItemData.builder()
                            .name(item.getName())
                            .price(item.getPrice())
                            .quantity(item.getQuantity())
                            .build());
                }
            } else {
                items.add(ItemData.builder()
                        .name("Thanh toán đơn hàng #" + request.getOrderCode())
                        .price(request.getAmount())
                        .quantity(1)
                        .build());
            }

            PaymentData paymentData = PaymentData.builder()
                    .orderCode(request.getOrderCode())
                    .amount(request.getAmount())
                    .description(request.getDescription())
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .items(items)
                    .build();
            if (request.getBuyerName() != null)
                paymentData.setBuyerName(request.getBuyerName());
            if (request.getBuyerEmail() != null)
                paymentData.setBuyerEmail(request.getBuyerEmail());
            if (request.getBuyerPhone() != null)
                paymentData.setBuyerPhone(request.getBuyerPhone());
            if (request.getBuyerAddress() != null)
                paymentData.setBuyerAddress(request.getBuyerAddress());

            CheckoutResponseData response = payOS.createPaymentLink(paymentData);
            if (response != null && response.getCheckoutUrl() != null) {
                log.info("PayOS payment link created successfully: {}", response.getCheckoutUrl());
                return response.getCheckoutUrl();
            } else {
                log.error("PayOS returned null or empty checkout URL");
                throw new AppException(ErrorCode.PAYMENT_ERROR);
            }
        } catch (Exception e) {
            log.error("Error creating PayOS payment link: {}", e.getMessage(), e);
            throw e;
        }
    }

    public boolean validateWebhook(PayOSWebhookRequest request) {
        try {
            // Kiểm tra nếu là webhook test (không có data hoặc data rỗng)
            if (request.getData() == null || request.getData().isEmpty()) {
                log.info("Received PayOS configuration test webhook");
                return true; // Chấp nhận webhook test
            }

            // Kiểm tra xem orderCode có tồn tại trong database không (nếu có)
            if (request.getData().containsKey("orderCode")) {
                String orderCode = request.getData().get("orderCode").toString();
                try {
                    Long orderId = Long.parseLong(orderCode);
                    // Kiểm tra đơn hàng tồn tại trong database không
                    try {
                        orderRepository.findById(orderId)
                                .orElseThrow(() -> {
                                    log.info(
                                            "Order ID {} from PayOS webhook not found in database. This is likely a test webhook.",
                                            orderId);
                                    return null; // Không ném exception, vẫn tiếp tục xử lý
                                });
                    } catch (Exception e) {
                        // Bỏ qua lỗi này, vẫn tiếp tục xác thực
                        log.info("Order lookup failed, but still validating signature: {}", e.getMessage());
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid orderCode format in PayOS webhook: {}", orderCode);
                    // Vẫn tiếp tục xác thực
                }
            }
            return isValidWebhookSignature(request.getData(), request.getSignature());
        } catch (Exception e) {
            log.error("Error validating webhook", e);
            return false;
        }
    }

    /**
     * Xác thực chữ ký webhook từ PayOS sử dụng HMAC-SHA256
     *
     * @param data              Dữ liệu webhook
     * @param receivedSignature Chữ ký nhận được từ webhook
     * @return true nếu chữ ký hợp lệ, false nếu không hợp lệ
     */
    public boolean isValidWebhookSignature(Map<String, Object> data, String receivedSignature) {
        try {
            if (data == null || receivedSignature == null) {
                log.error("Missing data or signature");
                return false;
            }

            log.info("Validating webhook signature with data: {}", data);

            // Sắp xếp các key theo thứ tự alphabet
            List<String> keys = new ArrayList<>(data.keySet());
            Collections.sort(keys);

            // Tạo chuỗi dữ liệu để tính toán chữ ký
            StringBuilder dataStr = new StringBuilder();
            for (int i = 0; i < keys.size(); i++) {
                String key = keys.get(i);
                Object value = data.get(key);
                dataStr.append(key);
                dataStr.append('=');
                dataStr.append(value != null ? value.toString() : "");
                if (i < keys.size() - 1) {
                    dataStr.append('&');
                }
            }

            String dataToSign = dataStr.toString();
            log.debug("Data to sign: {}", dataToSign);

            // Tính toán chữ ký sử dụng HMAC-SHA256
            String calculatedSignature = new HmacUtils("HmacSHA256", payOSConfig.getChecksumKey()).hmacHex(dataToSign);
            log.debug("Calculated signature: {}", calculatedSignature);
            log.debug("Received signature: {}", receivedSignature);

            boolean isValid = calculatedSignature.equals(receivedSignature);
            if (!isValid) {
                log.warn("Invalid webhook signature");
            }

            return isValid;
        } catch (Exception e) {
            log.error("Error validating webhook signature", e);
            return false;
        }
    }

    @Transactional
    public void processPayment(Map<String, Object> payload) {
        try {
            log.info("Processing payment payload: {}", payload);

            // Kiểm tra null và empty payload
            if (payload == null || payload.isEmpty()) {
                log.warn("Payment payload is null or empty. Skipping processing.");
                return;
            }

            // Extract data directly from payload
            Object statusObj = payload.get("status");
            Object orderCodeObj = payload.get("orderCode");

            if (statusObj == null || orderCodeObj == null) {
                log.warn("Missing 'status' or 'orderCode' in payload");
                return;
            }
            Long orderCode = null;
            try {
                orderCode = Long.parseLong(orderCodeObj.toString());
            } catch (NumberFormatException e) {
                log.error("Invalid orderCode format: {}", orderCodeObj);
                return;
            }

            // Tìm đơn hàng theo orderCode
            Order order = null;
            try {
                order = orderRepository.findById(orderCode)
                        .orElse(null);

                if (order == null) {
                    log.info("Order {} not found. This may be a test webhook. Skipping processing.", orderCode);
                    return;
                }
            } catch (Exception e) {
                log.warn("Error finding order {}: {}. This may be a test webhook.", orderCode, e.getMessage());
                return;
            }

            String status = statusObj.toString();
            if ("PAID".equals(status)) {
                order.setStatus(Order.OrderStatus.CONFIRMED);
                order.setPaymentStatus("PAID");
                log.info("Order {} updated to CONFIRMED", orderCode);
            } else if ("CANCELLED".equals(status)) {
                order.setStatus(Order.OrderStatus.CANCELLED);
                log.info("Order {} updated to CANCELLED", orderCode);
            } else if ("WAITING".equals(status)) {
                order.setStatus(Order.OrderStatus.PENDING);
                log.info("Order {} is WAITING", orderCode);
            } else {
                log.warn("Unknown payment status: {}", status);
            }

            orderRepository.save(order);
            log.info("Saved order {}", orderCode);
        } catch (Exception e) {
            log.error("Error processing payment", e);
        }
    }
}