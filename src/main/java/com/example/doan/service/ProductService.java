package com.example.doan.service;

import com.example.doan.dto.request.ProductRequest;
import com.example.doan.entity.Category;
import com.example.doan.entity.Product;
import com.example.doan.entity.ProductImage;
import com.example.doan.entity.OrderItem;
import com.example.doan.repository.OrderItemRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import com.example.doan.exception.AppException;
import com.example.doan.exception.ErrorCode;
import com.example.doan.repository.CategoryRepository;
import com.example.doan.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public Product createProduct(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Product product = Product.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .price(request.getPrice())
                .discountPrice(request.getDiscountPrice())
                .thumbnailUrl(request.getThumbnailUrl())
                .material(request.getMaterial())
                .dimensions(request.getDimensions())
                .quantityInStock(request.getQuantityInStock())
                .category(category)
                .build();

        Product createdProduct = productRepository.save(product); // ✅ sử dụng biến mới

        // Lưu ảnh chi tiết nếu có
        if (request.getImages() != null) {
            List<ProductImage> images = request.getImages().stream()
                    .map(url -> ProductImage.builder()
                            .imageUrl(url)
                            .product(createdProduct) // ✅ không còn lỗi
                            .build())
                    .toList();

            createdProduct.setImages(images);
        }

        return createdProduct;
    }

    @Transactional
    public Product updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        product.setName(request.getName());
        product.setSlug(request.getSlug());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setDiscountPrice(request.getDiscountPrice());
        product.setThumbnailUrl(request.getThumbnailUrl());
        product.setMaterial(request.getMaterial());
        product.setDimensions(request.getDimensions());
        product.setQuantityInStock(request.getQuantityInStock());
        product.setCategory(category);
        product.setUpdatedAt(LocalDateTime.now());

        // Xóa ảnh cũ và thêm ảnh mới nếu có
        product.getImages().clear();
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            request.getImages().forEach(url -> product.getImages().add(new ProductImage(null, url, product)));
        }

        return productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Sản phẩm không tồn tại");
        }
        productRepository.deleteById(id);
    }

    public List<Product> getAll() {
        // This method is likely for admin, should eager load category and images
        // It currently uses the default findAll() which might not eager load everything
        // Let's create a dedicated method for admin.
        return productRepository.findAll();
    }

    // New method for admin to get all products with eager loaded category and
    // images
    public List<Product> getAllProductsForAdmin() {
        // Call the overridden findAll() method in ProductRepository which has the
        // EntityGraph for category and images
        return productRepository.findAll();
    }

    public Page<Product> searchProducts(String keyword, Long categoryId, Pageable pageable) {
        // If both keyword and categoryId are null or empty, return all products
        // paginated with Category eagerly fetched
        if ((keyword == null || keyword.trim().isEmpty()) && categoryId == null) {
            return productRepository.findAll(pageable); // Use the overridden findAll with EntityGraph
        }

        if (categoryId != null) {
            // Search by keyword within a specific category
            // Need to handle the case where keyword is null when categoryId is not
            if (keyword == null || keyword.trim().isEmpty()) {
                return productRepository.findByCategoryId(categoryId, pageable); // Assuming findByCategoryId method
                // exists
            } else {
                return productRepository.findByNameContainingIgnoreCaseAndCategoryId(keyword, categoryId, pageable);
            }
        } else { // categoryId is null, but keyword is not null/empty
            return productRepository.findByNameContainingIgnoreCase(keyword, pageable); // Search by keyword across all
            // categories
        }
    }

    public List<Product> getTopSellingProducts(int topN) {
        List<OrderItem> allItems = orderItemRepository.findAll();
        Map<Product, Integer> productSales = new HashMap<>();
        for (OrderItem item : allItems) {
            productSales.merge(item.getProduct(), item.getQuantity(), Integer::sum);
        }
        return productSales.entrySet().stream()
                .sorted(Map.Entry.<Product, Integer>comparingByValue().reversed())
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public Product getById(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
    }
}
