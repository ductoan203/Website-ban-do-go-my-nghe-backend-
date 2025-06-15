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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.LocalTime;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ReportResponse getReportData(String startDateStr, String endDateStr, String status) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate startDate = LocalDate.parse(startDateStr, formatter);
        LocalDate endDate = LocalDate.parse(endDateStr, formatter);

        // Convert LocalDate to Instant for database query (start of day for startDate,
        // end of day for endDate)
        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant();

        List<Order> orders;
        if (StringUtils.hasText(status)) {
            orders = orderRepository.findByCreatedAtBetweenAndStatus(startInstant, endInstant,
                    Order.OrderStatus.valueOf(status.toUpperCase()));
        } else {
            orders = orderRepository.findByCreatedAtBetween(startInstant, endInstant);
        }

        BigDecimal totalSales = orders.stream()
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalOrders = orders.size();
        BigDecimal averageOrderValue = totalOrders == 0 ? BigDecimal.ZERO
                : totalSales.divide(BigDecimal.valueOf(totalOrders), 0, BigDecimal.ROUND_HALF_UP);

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
                categorySales.put(cat.getName(), categorySales.get(cat.getName())
                        .add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))));
            }
        }

        BigDecimal totalCatSales = categorySales.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<ReportResponse.CategorySales> salesByCategory = categorySales.entrySet().stream()
                .map(e -> ReportResponse.CategorySales.builder()
                        .name(e.getKey())
                        .sales(e.getValue())
                        .percentage(totalCatSales.compareTo(BigDecimal.ZERO) == 0 ? 0
                                : e.getValue().multiply(BigDecimal.valueOf(100))
                                .divide(totalCatSales, 2, BigDecimal.ROUND_HALF_UP).doubleValue())
                        .build())
                .toList();

        // Daily sales calculation for the selected range
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM");
        Map<String, BigDecimal> dailyMap = new TreeMap<>();

        // Initialize dailyMap with all days in the range to ensure all days are
        // present, even if no sales
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            dailyMap.put(current.format(DateTimeFormatter.ofPattern("dd/MM")), BigDecimal.ZERO);
            current = current.plusDays(1);
        }

        for (Order order : orders) {
            // Use LocalDate to get just the date part of the order's creation time
            LocalDate orderDate = order.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
            String formattedDate = orderDate.format(DateTimeFormatter.ofPattern("dd/MM"));
            dailyMap.put(formattedDate, dailyMap.getOrDefault(formattedDate, BigDecimal.ZERO).add(order.getTotal()));
        }

        List<ReportResponse.DailySales> dailySales = dailyMap.entrySet().stream()
                .map(e -> ReportResponse.DailySales.builder().date(e.getKey()).sales(e.getValue()).build())
                .collect(Collectors.toList()); // Use collect(Collectors.toList()) instead of toList() for older Java
        // versions if needed

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
