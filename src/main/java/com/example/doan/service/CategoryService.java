package com.example.doan.service;

import com.example.doan.dto.request.CategoryRequest;
import com.example.doan.entity.Category;
import com.example.doan.exception.AppException;
import com.example.doan.exception.ErrorCode;
import com.example.doan.repository.CategoryRepository;
import com.example.doan.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public Category create(CategoryRequest request) {
        if (categoryRepository.findByName(request.getName()).isPresent()) {
            throw new AppException(ErrorCode.INVALID_CATEGORY_NAME);
        }
        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        return categoryRepository.save(category);
    }

    public List<Category> getAll(String searchTerm) {
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            return categoryRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(searchTerm,
                    searchTerm);
        } else {
            return categoryRepository.findAll();
        }
    }

    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new AppException(ErrorCode.CATEGOTY_NOT_FOUND);
        }
        if (productRepository.existsByCategoryId(id)) {
            throw new AppException(ErrorCode.CATEGORY_DELETE_FAILED);
        }
        categoryRepository.deleteById(id);
    }

    public Category update(Long id, CategoryRequest request) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGOTY_NOT_FOUND));

        // Kiểm tra tên danh mục trùng lặp, bỏ qua danh mục hiện tại
        if (categoryRepository.findByName(request.getName()).isPresent() &&
                !existingCategory.getName().equals(request.getName())) {
            throw new AppException(ErrorCode.INVALID_CATEGORY_NAME);
        }

        existingCategory.setName(request.getName());
        existingCategory.setDescription(request.getDescription());

        return categoryRepository.save(existingCategory);
    }
}
