package com.example.doan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.example.doan.dto.response.CategoryResponse;
import com.example.doan.dto.response.ProductImageResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
}