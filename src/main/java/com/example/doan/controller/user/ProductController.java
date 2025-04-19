package com.example.doan.controller.user;

import com.example.doan.dto.request.ApiResponse;
import com.example.doan.entity.Product;
import com.example.doan.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // Lấy tất cả sản phẩm (đơn giản)
    @GetMapping("/all")
    public ApiResponse<List<Product>> getAllProducts() {
        return ApiResponse.<List<Product>>builder()
                .result(productService.getAll())
                .build();
    }

    // Lấy theo phân trang + tìm kiếm + danh mục
//    @GetMapping
//    public ApiResponse<Page<Product>> searchProducts(
//            @RequestParam(defaultValue = "") String keyword,
//            @RequestParam(required = false) Long categoryId,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size
//    ) {
//        Pageable pageable = PageRequest.of(page, size);
//        return ApiResponse.<Page<Product>>builder()
//                .result(productService.searchProducts(keyword, categoryId, pageable))
//                .build();
//    }

    // Xem chi tiết sản phẩm
    @GetMapping("/{id}")
    public ApiResponse<Product> getProductById(@PathVariable Long id) {
        return ApiResponse.<Product>builder()
                .result(productService.getById(id))
                .build();
    }
}
