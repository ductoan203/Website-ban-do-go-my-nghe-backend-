package com.example.doan.controller.admin;

import com.example.doan.dto.request.ApiResponse;
import com.example.doan.dto.response.OrderResponse;
import com.example.doan.entity.Order;
import com.example.doan.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {
    private final OrderService orderService;

    @GetMapping()
    public ApiResponse<List<OrderResponse>> getAllOrders(@RequestParam(required = false) Order.OrderStatus status) {
        return ApiResponse.<List<OrderResponse>>builder()
                .result(orderService.getAllOrdersByStatus(status))
                .build();
    }


    @PutMapping("/{id}/status")
    public ApiResponse<OrderResponse> updateStatus(@PathVariable Long id, @RequestParam Order.OrderStatus status) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.convertToDto(orderService.updateStatus(id, status)))
                .build();
    }
}
