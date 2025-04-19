package com.example.doan.controller.user;

import com.example.doan.dto.request.ApiResponse;
import com.example.doan.dto.request.OrderRequest;
import com.example.doan.dto.response.OrderResponse;
import com.example.doan.entity.Order;
import com.example.doan.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/checkout")
    public ApiResponse<OrderResponse> placeOrder(@RequestBody OrderRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.convertToDto(orderService.placeOrder(username, request)))
                .build();
    }

    @GetMapping("/me")
    public ApiResponse<List<OrderResponse>> getMyOrders(@RequestParam(required = false) Order.OrderStatus status) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.<List<OrderResponse>>builder()
                .result(orderService.getMyOrdersByStatus(username, status))
                .build();
    }


    @PutMapping("/cancel/{id}")
    public ApiResponse<OrderResponse> cancelOrder(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.convertToDto(orderService.cancelOrder(username, id)))
                .build();
    }

}