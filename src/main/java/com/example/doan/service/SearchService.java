package com.example.doan.service;

import com.example.doan.dto.response.SearchResponse;
import com.example.doan.dto.response.ProductResponse;
import com.example.doan.dto.response.CategoryResponse;
import com.example.doan.entity.Category;
import com.example.doan.entity.Product;
import com.example.doan.repository.CategoryRepository;
import com.example.doan.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public SearchResponse search(String searchTerm) {
        List<Product> products = productRepository
                .findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(searchTerm,
                        searchTerm);
        List<Category> categories = categoryRepository
                .findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(searchTerm,
                        searchTerm);

        List<ProductResponse> productResponses = products.stream()
                .map(ProductResponse::fromProduct)
                .toList();
        List<CategoryResponse> categoryResponses = categories.stream()
                .map(CategoryResponse::fromCategory)
                .toList();

        return SearchResponse.builder()
                .products(productResponses)
                .categories(categoryResponses)
                .build();
    }
}