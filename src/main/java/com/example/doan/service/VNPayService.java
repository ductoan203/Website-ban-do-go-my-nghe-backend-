package com.example.doan.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import com.example.doan.config.VNPayConfig;

@Getter
@Setter
@Service
public class VNPayService {

    private static final Logger log = LoggerFactory.getLogger(VNPayService.class);

    private final VNPayConfig vnpayConfig;

    public VNPayService(VNPayConfig vnpayConfig) {
        this.vnpayConfig = vnpayConfig;
    }

    public String createPaymentUrl(HttpServletRequest request, long amount, String orderId, String orderInfo,
                                   String bankCode, String expireDate) {
        try {
            String vnp_IpAddr = getIpAddress(request);
            String vnp_CreateDate = getCurrentDate();

            Map<String, String> vnp_Params = new LinkedHashMap<>();
            vnp_Params.put("vnp_Version", "2.1.0");
            vnp_Params.put("vnp_Command", "pay");
            vnp_Params.put("vnp_TmnCode", vnpayConfig.getTmnCode());
            vnp_Params.put("vnp_Amount", String.valueOf(amount * 100));
            vnp_Params.put("vnp_CurrCode", "VND");
            vnp_Params.put("vnp_TxnRef", orderId);
            vnp_Params.put("vnp_OrderInfo", orderInfo);
            vnp_Params.put("vnp_OrderType", "other");
            vnp_Params.put("vnp_ReturnUrl", vnpayConfig.getReturnUrl());
            vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
            vnp_Params.put("vnp_Locale", "vn");
            if (bankCode != null && !bankCode.isEmpty()) {
                vnp_Params.put("vnp_BankCode", bankCode);
            }
            if (expireDate != null && !expireDate.isEmpty()) {
                vnp_Params.put("vnp_ExpireDate", expireDate);
            }

            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            for (int i = 0; i < fieldNames.size(); i++) {
                String fieldName = fieldNames.get(i);
                String fieldValue = vnp_Params.get(fieldName);
                if (fieldValue != null && fieldValue.length() > 0) {
                    hashData.append(fieldName).append("=").append(fieldValue);
                }
                if (i < fieldNames.size() - 1) {
                    hashData.append("&");
                }
            }
            String vnp_SecureHash = hmacSHA512(vnpayConfig.getHashSecret(), hashData.toString());
            log.info("🌐 [VNPAY] HashData: {}", hashData.toString());
            log.info("🌐 [VNPAY] vnp_SecureHash: {}", vnp_SecureHash);

            StringBuilder query = new StringBuilder();
            for (int i = 0; i < fieldNames.size(); i++) {
                String fieldName = fieldNames.get(i);
                String fieldValue = vnp_Params.get(fieldName);
                if (fieldValue != null && fieldValue.length() > 0) {
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8)).append("=")
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                    if (i < fieldNames.size() - 1) {
                        query.append("&");
                    }
                }
            }
            if (query.length() > 0)
                query.append("&");
            query.append("vnp_SecureHash=").append(vnp_SecureHash);

            String finalUrl = vnpayConfig.getPayUrl() + "?" + query.toString();
            log.info("🌐 [VNPAY] Final URL: {}", finalUrl);
            return finalUrl;
        } catch (Exception e) {
            throw new RuntimeException("Cannot create VNPAY URL", e);
        }
    }

    public String createPaymentUrlWithOrderInfo(HttpServletRequest request,
                                                com.example.doan.dto.request.OrderRequest orderRequest, String orderInfo) {
        try {
            String vnp_IpAddr = "127.0.0.1";
            String vnp_CreateDate = getCurrentDate();
            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", "2.1.0");
            vnp_Params.put("vnp_Command", "pay");
            vnp_Params.put("vnp_TmnCode", vnpayConfig.getTmnCode());
            vnp_Params.put("vnp_Amount",
                    String.valueOf(orderRequest.getTotal().multiply(BigDecimal.valueOf(100)).longValue()));
            vnp_Params.put("vnp_CurrCode", "VND");
            String orderId = UUID.randomUUID().toString();
            vnp_Params.put("vnp_TxnRef", orderId);
            vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang #" + orderId);
            vnp_Params.put("vnp_OrderType", "other");
            vnp_Params.put("vnp_Locale", "vn");
            vnp_Params.put("vnp_ReturnUrl", vnpayConfig.getReturnUrl());
            vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
            String expireDate = LocalDateTime.now().plusMinutes(15).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            vnp_Params.put("vnp_ExpireDate", expireDate);


            // 1. KHÔNG thêm vnp_SecureHash vào map khi tạo hashData
            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
//            for (int i = 0; i < fieldNames.size(); i++) {
//                String fieldName = fieldNames.get(i);
//                String fieldValue = vnp_Params.get(fieldName);
//                if (fieldValue != null && fieldValue.length() > 0) {
//                    hashData.append(fieldName).append("=").append(fieldValue);
//                }
//                if (i < fieldNames.size() - 1) {
//                    hashData.append("&");
//                }
//            }
            StringBuilder query = new StringBuilder();
            Iterator itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = (String) itr.next();
                String fieldValue = (String) vnp_Params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    //Build hash data
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    //Build query
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }
            String vnp_SecureHash = hmacSHA512(vnpayConfig.getHashSecret(), hashData.toString());
            log.info("🌐 [VNPAY] HashData: {}", hashData.toString());
            log.info("🌐 [VNPAY] vnp_SecureHash: {}", vnp_SecureHash);

            // 2. Tạo query string (chỉ encode ở đây)
//            StringBuilder query = new StringBuilder();
//            for (int i = 0; i < fieldNames.size(); i++) {
//                String fieldName = fieldNames.get(i);
//                String fieldValue = vnp_Params.get(fieldName);
//                if (fieldValue != null && fieldValue.length() > 0) {
//                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8)).append("=")
//                            .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
//                    if (i < fieldNames.size() - 1) {
//                        query.append("&");
//                    }
//                }
//            }
//            if (query.length() > 0)
//                query.append("&");
            String queryUrl = query.toString();
//            String vnp_SecureHash = VNPayConfig.hmacSHA512(VNPayConfig.secretKey, hashData.toString());
            queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
//            query.append("vnp_SecureHash=").append(vnp_SecureHash);

            String finalUrl = vnpayConfig.getPayUrl() + "?" + queryUrl;
            log.info("🌐 [VNPAY] Final URL: {}", finalUrl);
            return finalUrl;
        } catch (Exception e) {
            throw new RuntimeException("Cannot create VNPAY URL", e);
        }
    }

    private String getCurrentDate() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }

    private String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        // Có thể X-FORWARDED-FOR chứa nhiều IP, lấy cái đầu tiên
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0];
        }
        return ipAddress;
    }

    public static String hmacSHA512(String key, String data) throws Exception {
        Mac hmac512 = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        hmac512.init(secretKey);
        byte[] bytes = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hash = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1)
                hash.append('0');
            hash.append(hex);
        }
        return hash.toString().toUpperCase();
    }

    public String getHashSecret() {
        return this.vnpayConfig.getHashSecret();
    }
}