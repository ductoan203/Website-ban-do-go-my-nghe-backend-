package com.example.doan.controller.user;

import com.example.doan.dto.request.ApiResponse;
import com.example.doan.dto.request.CartItemRequest;
import com.example.doan.dto.response.CartResponse;
import com.example.doan.dto.response.OrderResponse;
import com.example.doan.entity.Cart;
import com.example.doan.entity.Order;
import com.example.doan.service.CartService;
import com.example.doan.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;
    private final OrderService orderService;

    @GetMapping
    public ApiResponse<CartResponse> getMyCart() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.<CartResponse>builder()
                .result(cartService.convertToResponse(cartService.getMyCart(username)))
                .build();
    }
    @GetMapping("/my")
    public ApiResponse<List<OrderResponse>> getMyOrders(@RequestParam(required = false) Order.OrderStatus status) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.<List<OrderResponse>>builder()
                .result(orderService.getMyOrdersByStatus(username, status))
                .build();
    }


    @PostMapping("/add")
    public ApiResponse<CartResponse> addToCart(@RequestBody CartItemRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.<CartResponse>builder()
                .result(cartService.convertToResponse(cartService.addToCart(username, request)))
                .build();
    }

    @PutMapping("/update")
    public ApiResponse<CartResponse> updateCartItem(@RequestBody CartItemRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.<CartResponse>builder()
                .result(cartService.convertToResponse(cartService.updateCartItem(username, request)))
                .build();
    }

    @DeleteMapping("/delete/{productId}")
    public ApiResponse<String> removeFromCart(@PathVariable Long productId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        cartService.removeFromCart(username, productId);
        return ApiResponse.<String>builder()
                .result("Đã xoá sản phẩm khỏi giỏ hàng")
                .build();
    }
}
