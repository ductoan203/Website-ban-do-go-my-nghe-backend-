package com.example.doan.controller.admin;

import com.example.doan.dto.request.ApiResponse;
import com.example.doan.dto.response.OrderResponse;
import com.example.doan.entity.Order;
import com.example.doan.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {
    private final OrderService orderService;

    @GetMapping()
    public ApiResponse<Page<OrderResponse>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) Order.OrderStatus status) {
        return ApiResponse.<Page<OrderResponse>>builder()
                .result(orderService.getAllOrdersPaged(page, size, status))
                .build();
    }

    @PutMapping("/{id}/status")
    public ApiResponse<OrderResponse> updateStatus(@PathVariable Long id, @RequestParam Order.OrderStatus status) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.convertToDto(orderService.updateStatus(id, status)))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrderById(id);
        return ApiResponse.<String>builder()
                .result("Xóa đơn hàng thành công")
                .build();
    }

    @PutMapping("/{id}/return")
    public ApiResponse<OrderResponse> returnOrder(@PathVariable Long id) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.convertToDto(orderService.returnOrder(id)))
                .build();
    }

}
