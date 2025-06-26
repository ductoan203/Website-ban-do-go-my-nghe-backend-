package com.example.doan.dto.response;

import com.example.doan.entity.Category;
import com.example.doan.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {
    private List<ProductResponse> products;
    private List<CategoryResponse> categories;
}