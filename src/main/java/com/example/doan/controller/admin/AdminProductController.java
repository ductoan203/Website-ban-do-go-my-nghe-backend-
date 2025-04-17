package com.example.doan.controller.admin;

import com.example.doan.dto.request.ApiResponse;
import com.example.doan.dto.request.ProductRequest;
import com.example.doan.entity.Product;
import com.example.doan.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductController {
    private final ProductService productService;

    @PostMapping
    public ApiResponse<Product> create(@RequestBody @Valid ProductRequest request) {
        return ApiResponse.<Product>builder()
                .result(productService.createProduct(request))
                .build();
    }

    // ✅ Lấy toàn bộ sản phẩm cho admin
    @GetMapping
    public ApiResponse<List<Product>> getAllForAdmin() {
        return ApiResponse.<List<Product>>builder()
                .result(productService.getAll())
                .build();
    }

    // ✅ Lấy sản phẩm theo id cho admin
    @GetMapping("/{id}")
    public ApiResponse<Product> getById(@PathVariable Long id) {
        return ApiResponse.<Product>builder()
                .result(productService.getById(id))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<Product> update(@PathVariable Long id, @RequestBody @Valid ProductRequest request) {
        return ApiResponse.<Product>builder()
                .result(productService.updateProduct(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ApiResponse.<String>builder()
                .result("Xoá sản phẩm thành công")
                .build();
    }
}
