package com.example.doan.controller.user;

import com.example.doan.dto.request.ApiResponse;
import com.example.doan.dto.request.CommentRequest;
import com.example.doan.entity.Product;
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

    // Lấy tất cả sản phẩm (đơn giản) - Keep this for potential direct access if
    // needed
    @GetMapping("/all")
    public ApiResponse<List<Product>> getAllProductsSimple() { // Renamed to avoid conflict
        return ApiResponse.<List<Product>>builder()
                .result(productService.getAll()) // Assuming getAll returns List<Product>
                .build();
    }

    // Lấy sản phẩm theo danh mục, giới hạn top N HOẶC lấy tất cả sản phẩm có phân
    // trang
    @GetMapping
    public ApiResponse<?> getProducts( // Use wildcard or specific type if possible
                                       @RequestParam(required = false) Long categoryId,
                                       @RequestParam(required = false, defaultValue = "0") int page, // Add pagination params
                                       @RequestParam(required = false, defaultValue = "10") int size, // Add pagination params
                                       @RequestParam(required = false, defaultValue = "4") int top) { // top is only for category
        // filter

        if (categoryId != null) {
            // Existing logic for filtering by category and limiting by top
            List<Product> products = productService.getAll().stream()
                    .filter(p -> p.getCategory() != null
                            && p.getCategory().getId().equals(categoryId))
                    .limit(top) // Apply top limit for category filter
                    .toList();
            // Wrap the list in a structure expected by the frontend if needed, or adjust
            // frontend
            // For now, let's assume the frontend handles a simple list for category filter
            return ApiResponse.<List<Product>>builder()
                    .result(products)
                    .build();
        } else {
            // Logic for fetching all products with pagination using existing searchProducts
            Pageable pageable = PageRequest.of(page, size);
            Page<Product> paginatedProducts = productService.searchProducts(null, null, pageable); // Use
            // existing
            // searchProducts
            // method

            System.out.println("Debug: Paginated products content size: "
                    + paginatedProducts.getContent().size()); // Debug
            // log
            System.out.println(
                    "Debug: Paginated products total pages: " + paginatedProducts.getTotalPages()); // Debug
            // log
            System.out.println("Debug: Paginated products total elements: "
                    + paginatedProducts.getTotalElements()); // Debug
            // log

            // Return the paginated results in the expected format
            return ApiResponse.<Page<Product>>builder()
                    .result(paginatedProducts)
                    .build();
        }
    }

    // Xem chi tiết sản phẩm
    @GetMapping("/{id}")
    public ApiResponse<Product> getProductById(@PathVariable Long id) {
        try {
            return ApiResponse.<Product>builder()
                    .result(productService.getById(id))
                    .build();
        } catch (Exception e) {
            // Log the exception for debugging
            e.printStackTrace();
            // Re-throw or return an error response
            throw e;
        }
    }

    @GetMapping("/highlight")
    public ApiResponse<List<Product>> getHighlightProducts(@RequestParam(defaultValue = "4") int top) {
        try {
            return ApiResponse.<List<Product>>builder()
                    .result(productService.getTopSellingProducts(top))
                    .build();
        } catch (Exception e) {
            // Log the exception for debugging
            e.printStackTrace();
            // Re-throw or return an error response
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
            // Log the exception for debugging
            e.printStackTrace();
            // Re-throw or return an error response
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
        // The service method should handle checking if the comment belongs to the
        // current user
        return ApiResponse.<Comment>builder()
                .result(commentService.updateComment(commentId, request.getContent()))
                .build();
    }

    // Delete a comment
    @DeleteMapping("/{productId}/comments/{commentId}")
    public ApiResponse<Void> deleteProductComment(
            @PathVariable Long productId,
            @PathVariable Long commentId) {
        // The service method should handle checking if the comment belongs to the
        // current user
        commentService.deleteComment(commentId);
        return ApiResponse.<Void>builder()
                .message("Comment deleted successfully")
                .build();
    }
}