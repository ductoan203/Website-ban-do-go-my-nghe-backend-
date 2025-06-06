package com.example.doan.repository;

import com.example.doan.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);

    boolean existsById(Long id);

    List<Category> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String nameTerm,
                                                                                   String descriptionTerm);
}
