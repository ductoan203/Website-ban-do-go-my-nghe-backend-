package com.example.doan.repository;

import com.example.doan.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @EntityGraph(attributePaths = "user")
    List<Comment> findByProductIdOrderByCreatedAtDesc(Long productId);
}