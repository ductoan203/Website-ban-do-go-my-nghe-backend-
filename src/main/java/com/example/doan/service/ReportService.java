package com.example.doan.service;

import com.example.doan.dto.response.ReportResponse;
import com.example.doan.entity.Category;
import com.example.doan.entity.Order;
import com.example.doan.entity.OrderItem;
import com.example.doan.entity.Product;
import com.example.doan.repository.CategoryRepository;
import com.example.doan.repository.OrderRepository;
import com.example.doan.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ReportResponse getReportData(String timeRange) {
        Instant now = Instant.now();
        Instant start;

        switch (timeRange.toUpperCase()) {
            case "WEEK" -> start = now.minus(7, ChronoUnit.DAYS);
            case "MONTH" -> start = now.minus(30, ChronoUnit.DAYS);
            case "YEAR" -> start = now.minus(365, ChronoUnit.DAYS);
            default -> throw new IllegalArgumentException("Invalid timeRange: " + timeRange);
        }

        List<Order> orders = orderRepository.findByCreatedAtBetween(start, now);

        BigDecimal totalSales = orders.stream()
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalOrders = orders.size();
        BigDecimal averageOrderValue = totalOrders == 0 ? BigDecimal.ZERO : totalSales.divide(BigDecimal.valueOf(totalOrders), 0, BigDecimal.ROUND_HALF_UP);

        Map<Long, ReportResponse.TopProduct> productMap = new HashMap<>();
        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                long productId = item.getProduct().getId();
                productMap.putIfAbsent(productId,
                        ReportResponse.TopProduct.builder()
                                .name(item.getProduct().getName())
                                .sales(BigDecimal.ZERO)
                                .quantity(0)
                                .build());
                ReportResponse.TopProduct tp = productMap.get(productId);
                tp.setSales(tp.getSales().add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))));
                tp.setQuantity(tp.getQuantity() + item.getQuantity());
            }
        }

        List<ReportResponse.TopProduct> topProducts = productMap.values().stream()
                .sorted(Comparator.comparing(ReportResponse.TopProduct::getSales).reversed())
                .limit(5)
                .toList();

        Map<String, BigDecimal> categorySales = new HashMap<>();
        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                Category cat = item.getProduct().getCategory();
                categorySales.putIfAbsent(cat.getName(), BigDecimal.ZERO);
                categorySales.put(cat.getName(), categorySales.get(cat.getName()).add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))));
            }
        }

        BigDecimal totalCatSales = categorySales.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<ReportResponse.CategorySales> salesByCategory = categorySales.entrySet().stream()
                .map(e -> ReportResponse.CategorySales.builder()
                        .name(e.getKey())
                        .sales(e.getValue())
                        .percentage(totalCatSales.compareTo(BigDecimal.ZERO) == 0 ? 0 : e.getValue().multiply(BigDecimal.valueOf(100)).divide(totalCatSales, 2, BigDecimal.ROUND_HALF_UP).doubleValue())
                        .build())
                .toList();

        Map<String, BigDecimal> dailyMap = new TreeMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM");
        for (Order order : orders) {
            String date = sdf.format(Date.from(order.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant()));
            dailyMap.putIfAbsent(date, BigDecimal.ZERO);
            dailyMap.put(date, dailyMap.get(date).add(order.getTotal()));
        }

        List<ReportResponse.DailySales> dailySales = dailyMap.entrySet().stream()
                .map(e -> ReportResponse.DailySales.builder().date(e.getKey()).sales(e.getValue()).build())
                .toList();

        return ReportResponse.builder()
                .totalSales(totalSales)
                .totalOrders(totalOrders)
                .averageOrderValue(averageOrderValue)
                .topProducts(topProducts)
                .salesByCategory(salesByCategory)
                .dailySales(dailySales)
                .build();
    }
}
