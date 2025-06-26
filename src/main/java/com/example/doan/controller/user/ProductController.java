package com.example.doan.controller.user;

import com.example.doan.dto.request.ApiResponse;
import com.example.doan.dto.request.CommentRequest;
import com.example.doan.dto.response.ProductResponse;
import com.example.doan.dto.response.CommentResponse;
import com.example.doan.dto.response.UserResponse;
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
            @RequestParam(required = false, defaultValue = "4") int top,
            @RequestParam(required = false) String stockStatus,
            @RequestParam(required = false) String sort) {

        if (categoryId != null) {
            Pageable pageable = PageRequest.of(0, top);
            Page<ProductResponse> productsPage = productService.searchProducts(null, categoryId, pageable, stockStatus,
                    sort);
            return ApiResponse.<Page<ProductResponse>>builder()
                    .result(productsPage)
                    .build();
        } else {
            Pageable pageable = PageRequest.of(page, size);
            Page<ProductResponse> paginatedProducts = productService.searchProducts(null, null, pageable, stockStatus,
                    sort);

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
    public ApiResponse<List<CommentResponse>> getProductComments(@PathVariable Long productId) {
        try {
            List<Comment> comments = commentService.getCommentsByProductId(productId);
            List<CommentResponse> dtos = comments.stream().map(c -> new CommentResponse(
                    c.getId(),
                    c.getContent(),
                    c.getCreatedAt(),
                    new UserResponse(
                            c.getUser().getUserId(),
                            null, null, c.getUser().getFullname(), null, null, null, null),
                    null,
                    c.getParent() != null ? c.getParent().getId() : null)).toList();
            return ApiResponse.<List<CommentResponse>>builder()
                    .result(dtos)
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
    public ApiResponse<CommentResponse> updateProductComment(
            @PathVariable Long productId,
            @PathVariable Long commentId,
            @RequestBody CommentRequest request) {
        Comment updated = commentService.updateComment(commentId, request.getContent());
        // Map entity sang DTO
        CommentResponse dto = new CommentResponse(
                updated.getId(),
                updated.getContent(),
                updated.getCreatedAt(),
                new UserResponse(updated.getUser().getUserId(), null, null, updated.getUser().getFullname(), null, null,
                        null, null),
                null,
                null);
        return ApiResponse.<CommentResponse>builder()
                .result(dto)
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