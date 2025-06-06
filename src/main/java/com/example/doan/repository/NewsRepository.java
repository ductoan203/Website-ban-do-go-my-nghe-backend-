package com.example.doan.repository;

import com.example.doan.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NewsRepository extends JpaRepository<News, Long> {
    Optional<News> findBySlug(String slug);
}
