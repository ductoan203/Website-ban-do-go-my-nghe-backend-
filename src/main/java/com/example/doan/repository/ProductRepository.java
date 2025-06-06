package com.example.doan.repository;

import com.example.doan.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    boolean existsByCategoryId(Long categoryId);

    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCaseAndCategoryId(String name, Long categoryId, Pageable pageable);

    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    List<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String nameTerm,
                                                                                  String descriptionTerm);

    // Override the default findAll(Pageable) and apply EntityGraph to eager load
    // category for public product list
    @Override
    @EntityGraph(attributePaths = "category")
    Page<Product> findAll(Pageable pageable);

    // Override the default findAll() and apply EntityGraph to eager load
    // category and images for admin product list
    @Override
    @EntityGraph(attributePaths = { "category", "images" })
    List<Product> findAll();
}
