package com.example.doan.controller.admin;

import com.example.doan.dto.request.ApiResponse;
import com.example.doan.dto.response.OrderResponse;
import com.example.doan.service.OrderDashboardService;
import com.example.doan.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final OrderDashboardService dashboardService;
    private final OrderService orderService;

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> getOverview() {
        BigDecimal totalRevenue = dashboardService.getTotalRevenue();
        long totalOrders = dashboardService.getTotalOrders();
        Map<String, Long> statusCounts = dashboardService.getOrderCountByStatus();

        // Thêm doanh thu tháng hiện tại
        LocalDate now = LocalDate.now();
        BigDecimal monthlyRevenue = dashboardService.getMonthlyRevenue(now.getMonthValue(), now.getYear());

        // Lấy các đơn hàng gần đây (đã là OrderResponse DTO)
        List<OrderResponse> recentOrderResponses = dashboardService.getRecentOrders(5);

        return ApiResponse.<Map<String, Object>>builder()
                .result(Map.of(
                        "totalRevenue", totalRevenue,
                        "totalOrders", totalOrders,
                        "statusCounts", statusCounts,
                        "monthlyRevenue", monthlyRevenue, // Thêm doanh thu tháng
                        "recentOrders", recentOrderResponses // Thêm đơn hàng gần đây
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
