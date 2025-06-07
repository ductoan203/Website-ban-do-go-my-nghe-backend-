package com.example.doan.controller.user;

import com.example.doan.dto.request.ApiResponse;
import com.example.doan.dto.request.CommentRequest;
import com.example.doan.dto.response.ProductResponse;
import com.example.doan.entity.Comment;
import com.example.doan.service.ProductService;
import com.example.doan.service.CommentService;
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
    private final CommentService commentService;

    // Lấy tất cả sản phẩm (đơn giản)
    @GetMapping("/all")
    public ApiResponse<List<ProductResponse>> getAllProductsSimple() {
        return ApiResponse.<List<ProductResponse>>builder()
                .result(productService.getAll())
                .build();
    }

    // Lấy sản phẩm theo danh mục, giới hạn top N HOẶC lấy tất cả sản phẩm có phân
    // trang
    @GetMapping
    public ApiResponse<Page<ProductResponse>> getProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "4") int top) {

        if (categoryId != null) {
            Pageable pageable = PageRequest.of(0, top);
            Page<ProductResponse> productsPage = productService.searchProducts(null, categoryId, pageable);
            return ApiResponse.<Page<ProductResponse>>builder()
                    .result(productsPage)
                    .build();
        } else {
            Pageable pageable = PageRequest.of(page, size);
            Page<ProductResponse> paginatedProducts = productService.searchProducts(null, null, pageable);

            System.out.println("Debug: Paginated products content size: "
                    + paginatedProducts.getContent().size());
            System.out.println(
                    "Debug: Paginated products total pages: " + paginatedProducts.getTotalPages());
            System.out.println("Debug: Paginated products total elements: "
                    + paginatedProducts.getTotalElements());

            return ApiResponse.<Page<ProductResponse>>builder()
                    .result(paginatedProducts)
                    .build();
        }
    }

    // Xem chi tiết sản phẩm
    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProductById(@PathVariable Long id) {
        try {
            return ApiResponse.<ProductResponse>builder()
                    .result(productService.getById(id))
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @GetMapping("/highlight")
    public ApiResponse<List<ProductResponse>> getHighlightProducts(@RequestParam(defaultValue = "4") int top) {
        try {
            return ApiResponse.<List<ProductResponse>>builder()
                    .result(productService.getTopSellingProducts(top))
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    // Get comments for a product
    @GetMapping("/{productId}/comments")
    public ApiResponse<List<Comment>> getProductComments(@PathVariable Long productId) {
        try {
            return ApiResponse.<List<Comment>>builder()
                    .result(commentService.getCommentsByProductId(productId))
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    // Add a comment to a product
    @PostMapping("/{productId}/comments")
    public ApiResponse<Comment> addProductComment(
            @PathVariable Long productId,
            @RequestBody CommentRequest request) {
        return ApiResponse.<Comment>builder()
                .result(commentService.addCommentToProduct(productId, request.getContent()))
                .build();
    }

    // Update a comment
    @PutMapping("/{productId}/comments/{commentId}")
    public ApiResponse<Comment> updateProductComment(
            @PathVariable Long productId,
            @PathVariable Long commentId,
            @RequestBody CommentRequest request) {
        return ApiResponse.<Comment>builder()
                .result(commentService.updateComment(commentId, request.getContent()))
                .build();
    }

    // Delete a comment
    @DeleteMapping("/{productId}/comments/{commentId}")
    public ApiResponse<Void> deleteProductComment(
            @PathVariable Long productId,
            @PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ApiResponse.<Void>builder()
                .message("Comment deleted successfully")
                .build();
    }
}