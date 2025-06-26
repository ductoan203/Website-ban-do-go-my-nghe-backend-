package com.example.doan.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class PayOSCreateRequest {
    private Long orderCode;
    private int amount;
    private String description;
    private String buyerName;
    private String buyerEmail;
    private String buyerPhone;
    private String buyerAddress;
    private List<Item> items;
    private String signature;

    @Data
    public static class Item {
        private String name;
        private int quantity;
        private int price;
    }
}