package com.example.doan.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductRequest {
    private String name;
    private String slug;
    private String description;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private String thumbnailUrl;
    private String material;
    private String dimensions;
    private Integer quantityInStock;
    private Long categoryId;
    private List<String> images;
    //private List<String> imageUrls;
}
