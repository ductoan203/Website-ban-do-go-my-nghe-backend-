package com.example.doan.controller.admin;

import com.example.doan.dto.request.ApiResponse;
import com.example.doan.dto.request.CategoryRequest;
import com.example.doan.entity.Category;
import com.example.doan.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ApiResponse<Category> createCategory(@RequestBody CategoryRequest request) {
        return ApiResponse.<Category>builder()
                .result(categoryService.create(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<Category>> getAllCategories(@RequestParam(required = false) String searchTerm) {
        return ApiResponse.<List<Category>>builder()
                .result(categoryService.getAll(searchTerm))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.<String>builder()
                .result("Xoá danh mục thành công")
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<Category> updateCategory(@PathVariable Long id, @RequestBody CategoryRequest request) {
        return ApiResponse.<Category>builder()
                .result(categoryService.update(id, request))
                .build();
    }
}