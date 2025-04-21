package com.example.doan.service;

import com.example.doan.entity.Order;
import com.example.doan.entity.Order.OrderStatus;
import com.example.doan.entity.OrderItem;
import com.example.doan.entity.Product;
import com.example.doan.repository.OrderItemRepository;
import com.example.doan.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderDashboardService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    // ✅ Tổng doanh thu từ các đơn đã xử lý
    public BigDecimal getTotalRevenue() {
        List<Order> completedOrders = orderRepository.findByStatusIn(List.of(OrderStatus.CONFIRMED, OrderStatus.SHIPPED));
        return completedOrders.stream()
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

}
