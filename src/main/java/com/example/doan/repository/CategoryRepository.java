package com.example.doan.repository;

import com.example.doan.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    // Define custom query methods if needed
    // For example, you can add methods to find categories by name or other attributes
    // Example: List<Category> findByName(String name);
}
