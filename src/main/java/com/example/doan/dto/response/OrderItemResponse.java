package com.example.doan.dto.response;

import com.example.doan.entity.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {
    private Long productId;
    private String productName;
    private String imageUrl;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;

    public static OrderItemResponse fromOrderItem(OrderItem orderItem) {
        BigDecimal subtotal = orderItem.getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity()));
        return OrderItemResponse.builder()
                .productId(orderItem.getProduct().getId())
                .productName(orderItem.getProduct().getName())
                .imageUrl(orderItem.getProduct().getThumbnailUrl() != null
                        ? "http://localhost:8080/doan" + orderItem.getProduct().getThumbnailUrl()
                        : null)
                .price(orderItem.getPrice())
                .quantity(orderItem.getQuantity())
                .subtotal(subtotal)
                .build();
    }
}