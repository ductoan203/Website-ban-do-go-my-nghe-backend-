package com.example.doan.controller.user;

import com.example.doan.dto.request.ApiResponse;
import com.example.doan.entity.Category;
import com.example.doan.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<Category>> getAllPublicCategories() {
        return ApiResponse.<List<Category>>builder()
                .result(categoryService.getAll(null))
                .build();
    }
}
