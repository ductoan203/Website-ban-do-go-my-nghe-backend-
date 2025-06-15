package com.example.doan.dto.response;

import com.example.doan.entity.ProductImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImageResponse {
    private Long id;
    private String imageUrl;

    public static ProductImageResponse fromProductImage(ProductImage productImage) {
        return ProductImageResponse.builder()
                .id(productImage.getId())
                .imageUrl(productImage.getImageUrl())
                .build();
    }
}