package com.example.doan.service;

import com.example.doan.dto.response.OrderResponse;
import com.example.doan.entity.Order;
import com.example.doan.entity.Order.OrderStatus;
import com.example.doan.entity.OrderItem;
import com.example.doan.entity.Product;
import com.example.doan.repository.OrderItemRepository;
import com.example.doan.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderDashboardService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderService orderService;

    // ✅ Tổng doanh thu từ các đơn đã xử lý
    public BigDecimal getTotalRevenue() {
        List<Order> completedOrders = orderRepository
                .findByStatusIn(List.of(OrderStatus.CONFIRMED, OrderStatus.SHIPPED, OrderStatus.DELIVERED));
        return completedOrders.stream()
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ✅ Doanh thu theo tháng
    public BigDecimal getMonthlyRevenue(int month, int year) {
        LocalDateTime startOfMonth = LocalDateTime.of(year, month, 1, 0, 0, 0);
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusDays(1).withHour(23).withMinute(59).withSecond(59);

        Instant startInstant = startOfMonth.atZone(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endOfMonth.atZone(ZoneId.systemDefault()).toInstant();

        List<Order> monthlyOrders = orderRepository.findByCreatedAtBetweenAndStatusIn(
                startInstant, endInstant, List.of(OrderStatus.CONFIRMED, OrderStatus.SHIPPED, OrderStatus.DELIVERED));

        return monthlyOrders.stream()
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ✅ Đếm số đơn theo trạng thái
    public Map<String, Long> getOrderCountByStatus() {
        Map<String, Long> result = new HashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            Long count = orderRepository.countByStatus(status);
            result.put(status.name(), count);
        }
        return result;
    }

    // ✅ Đếm đơn hàng tổng cộng
    public long getTotalOrders() {
        return orderRepository.count();
    }

    // ✅ Top sản phẩm bán chạy (theo số lượng)
    public List<Map<String, Object>> getTopSellingProducts(int topN) {
        List<OrderItem> allItems = orderItemRepository.findAll();

        Map<Product, Integer> productSales = new HashMap<>();
        for (OrderItem item : allItems) {
            productSales.merge(item.getProduct(), item.getQuantity(), Integer::sum);
        }

        return productSales.entrySet().stream()
                .sorted(Map.Entry.<Product, Integer>comparingByValue().reversed())
                .limit(topN)
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("productId", entry.getKey().getId());
                    map.put("productName", entry.getKey().getName());
                    map.put("sold", entry.getValue());
                    return map;
                })
                .collect(Collectors.toList());
    }

    // ✅ Lấy các đơn hàng gần đây
    public List<OrderResponse> getRecentOrders(int limit) {
        return orderRepository.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent().stream()
                .map(orderService::convertToDto)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getDashboardData() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59)
                .withSecond(59);

        Instant startInstant = startOfMonth.atZone(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endOfMonth.atZone(ZoneId.systemDefault()).toInstant();

        // Lấy tổng doanh thu tháng này từ các đơn hàng đã hoàn thành
        BigDecimal totalRevenue = orderRepository.findByCreatedAtBetweenAndStatusIn(startInstant, endInstant,
                        List.of(OrderStatus.CONFIRMED, OrderStatus.SHIPPED, OrderStatus.DELIVERED))
                .stream()
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Lấy số đơn hàng tháng này (tổng đơn trong tháng)
        long totalOrdersMonth = orderRepository.countByCreatedAtBetween(startInstant, endInstant);

        // Lấy số đơn hàng mới trong ngày hôm nay
        LocalDateTime startOfDay = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = now.withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        Instant startOfDayInstant = startOfDay.atZone(ZoneId.systemDefault()).toInstant();
        Instant endOfDayInstant = endOfDay.atZone(ZoneId.systemDefault()).toInstant();
        long newOrdersToday = orderRepository.countByCreatedAtBetween(startOfDayInstant, endOfDayInstant);

        // Lấy đơn hàng gần đây
        List<OrderResponse> recentOrderResponses = orderRepository.findTop5ByOrderByCreatedAtDesc().stream()
                .map(orderService::convertToDto)
                .collect(Collectors.toList());

        return Map.of(
                "totalRevenue", totalRevenue,
                "totalOrdersMonth", totalOrdersMonth,
                "newOrdersToday", newOrdersToday,
                "recentOrders", recentOrderResponses);
    }

    public long countOrdersToday() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        Instant startOfDayInstant = startOfDay.atZone(ZoneId.systemDefault()).toInstant();
        Instant endOfDayInstant = endOfDay.atZone(ZoneId.systemDefault()).toInstant();
        return orderRepository.countByCreatedAtBetween(startOfDayInstant, endOfDayInstant);
    }
}
