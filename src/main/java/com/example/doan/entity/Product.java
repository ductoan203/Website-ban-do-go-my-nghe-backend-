//package com.example.doan.entity;
//
//import jakarta.persistence.*;
//import lombok.Data;
//
//@Entity
//@Data
//@Table(name = "products")
//public class Product {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long productId;
//
//    private String name;
//    private String description;
//    private double price;
//    private int stockQuantity;
//    private String color;
//    private String material;
//    private double weight;
//    private double length;
//    private double width;
//    private double height;
//
//    @ManyToOne
//    @JoinColumn(name = "category_id")
//    private Category category;
//}
