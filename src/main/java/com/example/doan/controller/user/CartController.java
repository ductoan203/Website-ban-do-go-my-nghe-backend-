package com.example.doan.controller.user;

import com.example.doan.dto.request.ApiResponse;
import com.example.doan.dto.request.CartItemRequest;
import com.example.doan.dto.response.CartResponse;
import com.example.doan.dto.response.OrderResponse;
import com.example.doan.entity.Cart;
import com.example.doan.entity.Order;
import com.example.doan.dto.request.CartItemRequest;
import com.example.doan.dto.response.CartResponse;
import com.example.doan.exception.AppException;
import com.example.doan.service.CartService;
import com.example.doan.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;
    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<?> getMyCart() {
        try {
            var cart = cartService.getMyCart(cartService.getCurrentUser());
            return ResponseEntity.ok(cartService.convertToResponse(cart));
        } catch (AppException e) {
            // Xử lý các lỗi nghiệp vụ cụ thể
            return ResponseEntity.status(e.getErrorCode().getStatusCode()).body(e.getMessage());
        } catch (Exception e) {
            // Xử lý các lỗi không xác định
            return ResponseEntity.status(500).body("Đã xảy ra lỗi không xác định khi lấy giỏ hàng: " + e.getMessage());
        }
    }

    // @GetMapping("/my")
    // public ApiResponse<List<OrderResponse>> getMyOrders(@RequestParam(required =
    // false) Order.OrderStatus status) {
    // String username =
    // SecurityContextHolder.getContext().getAuthentication().getName();
    // return ApiResponse.<List<OrderResponse>>builder()
    // .result(orderService.getMyOrdersByStatus(username, status))
    // .build();
    // }

    @PostMapping("/add")
    public ResponseEntity<CartResponse> addToCart(@RequestBody CartItemRequest request) {
        var cart = cartService.addToCart(request);
        return ResponseEntity.ok(cartService.convertToResponse(cart));
    }

    @PutMapping("/update")
    public ResponseEntity<CartResponse> updateQuantity(@RequestBody CartItemRequest request) {
        var cart = cartService.updateCartItem(request);
        return ResponseEntity.ok(cartService.convertToResponse(cart));
    }

    @DeleteMapping("/delete/{productId}")
    public ResponseEntity<CartResponse> removeItem(@PathVariable Long productId) {
        cartService.removeFromCart(productId);
        var cart = cartService.getMyCart(cartService.getCurrentUser());
        return ResponseEntity.ok(cartService.convertToResponse(cart));
    }

}
