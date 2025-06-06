// ReportResponse.java
package com.example.doan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    private BigDecimal totalSales;
    private int totalOrders;
    private BigDecimal averageOrderValue;
    private List<TopProduct> topProducts;
    private List<CategorySales> salesByCategory;
    private List<DailySales> dailySales;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TopProduct {
        private String name;
        private BigDecimal sales;
        private int quantity;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategorySales {
        private String name;
        private BigDecimal sales;
        private double percentage;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailySales {
        private String date;
        private BigDecimal sales;
    }
}
