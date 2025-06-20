package com.example.doan.service;

import com.example.doan.config.MomoConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.example.doan.dto.request.CartItemRequest;
import com.example.doan.entity.Product;
import com.example.doan.repository.ProductRepository;

@Service
@RequiredArgsConstructor
public class MomoService {
    private final MomoConfig momoConfig;
    private final ObjectMapper objectMapper;
    private final ProductRepository productRepository;

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        this.restTemplate = new RestTemplate();
    }

    public String createPaymentUrl(String orderId, Long amount) throws Exception {
        String requestId = UUID.randomUUID().toString();
        String momoOrderId = orderId;

        Map<String, Object> rawData = new HashMap<>();
        rawData.put("partnerCode", momoConfig.getPartnerCode());
        rawData.put("accessKey", momoConfig.getAccessKey());
        rawData.put("requestId", requestId);
        rawData.put("amount", String.valueOf(amount));
        rawData.put("orderId", momoOrderId);
        rawData.put("orderInfo", "Thanh toán đơn hàng " + orderId);
        rawData.put("redirectUrl", momoConfig.getRedirectUrl());
        rawData.put("ipnUrl", momoConfig.getIpnUrl());
        rawData.put("extraData", "");
        rawData.put("requestType", "captureWallet");
        rawData.put("lang", "vi");
        rawData.put("orderType", "all");

        String rawSignature = "accessKey=" + momoConfig.getAccessKey()
                + "&amount=" + amount
                + "&extraData="
                + "&ipnUrl=" + momoConfig.getIpnUrl()
                + "&orderId=" + momoOrderId
                + "&orderInfo=Thanh toán đơn hàng " + orderId
                + "&partnerCode=" + momoConfig.getPartnerCode()
                + "&redirectUrl=" + momoConfig.getRedirectUrl()
                + "&requestId=" + requestId
                + "&requestType=captureWallet";

        String signature = HmacUtils.hmacSha256Hex(momoConfig.getSecretKey(), rawSignature);
        rawData.put("signature", signature);

        String response = restTemplate.postForObject(
                momoConfig.getEndpoint(), rawData, String.class);

        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        return (String) responseMap.get("payUrl");
    }

    public String createPaymentUrlWithExtraData(String extraData,
                                                com.example.doan.dto.request.OrderRequest orderRequest) throws Exception {
        String requestId = UUID.randomUUID().toString();
        BigDecimal total = orderRequest.getTotal();
        if (total == null) {
            total = BigDecimal.ZERO;
            for (CartItemRequest item : orderRequest.getItems()) {
                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found"));
                BigDecimal price = product.getDiscountPrice() != null
                        && product.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0
                        ? product.getDiscountPrice()
                        : product.getPrice();
                total = total.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }
        if (total == null || total.intValue() < 1000) {
            throw new IllegalArgumentException("Số tiền thanh toán phải lớn hơn hoặc bằng 1000 VND");
        }
        String orderInfo = "Khách: " + orderRequest.getCustomerName()
                + ", SĐT: " + orderRequest.getPhone()
                + ", Tổng: " + String.format("%,d", total.intValue()) + "đ";
        Map<String, Object> rawData = new HashMap<>();
        rawData.put("partnerCode", momoConfig.getPartnerCode());
        rawData.put("accessKey", momoConfig.getAccessKey());
        rawData.put("requestId", requestId);
        rawData.put("amount", total.intValue());
        rawData.put("orderId", requestId);
        rawData.put("orderInfo", orderInfo);
        rawData.put("redirectUrl", momoConfig.getRedirectUrl());
        rawData.put("ipnUrl", momoConfig.getIpnUrl());
        rawData.put("extraData", extraData);
        rawData.put("requestType", "captureWallet");
        rawData.put("lang", "vi");
        rawData.put("orderType", "all");
        String rawSignature = "accessKey=" + momoConfig.getAccessKey()
                + "&amount=" + rawData.get("amount")
                + "&extraData=" + extraData
                + "&ipnUrl=" + momoConfig.getIpnUrl()
                + "&orderId=" + requestId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + momoConfig.getPartnerCode()
                + "&redirectUrl=" + momoConfig.getRedirectUrl()
                + "&requestId=" + requestId
                + "&requestType=captureWallet";
        String signature = HmacUtils.hmacSha256Hex(momoConfig.getSecretKey(), rawSignature);
        rawData.put("signature", signature);
        String response = restTemplate.postForObject(
                momoConfig.getEndpoint(), rawData, String.class);
        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        return (String) responseMap.get("payUrl");
    }

    public String getAccessKey() {
        return momoConfig.getAccessKey();
    }

    public String getSecretKey() {
        return momoConfig.getSecretKey();
    }

}
