package com.example.doan.repository;

import com.example.doan.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {
    // Define custom query methods if needed
}
