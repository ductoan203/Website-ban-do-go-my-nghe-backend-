package com.example.doan.controller.admin;

import com.example.doan.dto.request.ApiResponse;
import com.example.doan.service.OrderDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final OrderDashboardService dashboardService;

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> getOverview() {
        BigDecimal totalRevenue = dashboardService.getTotalRevenue();
        long totalOrders = dashboardService.getTotalOrders();
        Map<String, Long> statusCounts = dashboardService.getOrderCountByStatus();

        return ApiResponse.<Map<String, Object>>builder()
                .result(Map.of(
                        "totalRevenue", totalRevenue,
                        "totalOrders", totalOrders,
                        "statusCounts", statusCounts
                ))
                .build();
    }

    @GetMapping("/top-products")
    public ApiResponse<List<Map<String, Object>>> getTopSellingProducts(@RequestParam(defaultValue = "5") int top) {
        return ApiResponse.<List<Map<String, Object>>>builder()
                .result(dashboardService.getTopSellingProducts(top))
                .build();
    }
}
