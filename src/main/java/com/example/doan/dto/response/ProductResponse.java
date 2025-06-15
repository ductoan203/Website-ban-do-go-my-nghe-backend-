package com.example.doan.dto.response;

import com.example.doan.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.example.doan.dto.response.CategoryResponse;
import com.example.doan.dto.response.ProductImageResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private String thumbnailUrl;
    private String material;
    private String dimensions;
    private Integer quantityInStock;
    private CategoryResponse category; // Use CategoryResponse DTO
    private List<ProductImageResponse> images; // Use ProductImageResponse DTO
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductResponse fromProduct(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .price(product.getPrice())
                .discountPrice(product.getDiscountPrice())
                .thumbnailUrl(
                        product.getThumbnailUrl() != null ? "http://localhost:8080/doan" + product.getThumbnailUrl()
                                : null)
                .material(product.getMaterial())
                .dimensions(product.getDimensions())
                .quantityInStock(product.getQuantityInStock())
                .category(product.getCategory() != null ? CategoryResponse.fromCategory(product.getCategory()) : null)
                .images(product.getImages() != null ? product.getImages().stream()
                        .map(ProductImageResponse::fromProductImage)
                        .collect(Collectors.toList()) : null)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}