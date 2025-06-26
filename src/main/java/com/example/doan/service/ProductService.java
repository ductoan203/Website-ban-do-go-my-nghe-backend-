package com.example.doan.service;

import com.example.doan.dto.request.ProductRequest;
import com.example.doan.dto.response.CategoryResponse;
import com.example.doan.dto.response.ProductImageResponse;
import com.example.doan.dto.response.ProductResponse;
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
import org.springframework.data.domain.PageImpl;
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

    public List<ProductResponse> getAll() {
        return productRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getAllProductsForAdmin() {
        return productRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Page<ProductResponse> searchProducts(String keyword, Long categoryId, Pageable pageable, String stockStatus,
                                                String sort) {
        Page<Product> productPage;
        if ((keyword == null || keyword.trim().isEmpty()) && categoryId == null) {
            productPage = productRepository.findAll(pageable);
        } else if (categoryId != null) {
            if (keyword == null || keyword.trim().isEmpty()) {
                productPage = productRepository.findByCategoryId(categoryId, pageable);
            } else {
                productPage = productRepository.findByNameContainingIgnoreCaseAndCategoryId(keyword, categoryId,
                        pageable);
            }
        } else { // categoryId is null, but keyword is not null/empty
            productPage = productRepository.findByNameContainingIgnoreCase(keyword, pageable);
        }

        // Lọc theo tình trạng hàng
        List<Product> filtered = productPage.getContent();
        if (stockStatus != null) {
            if (stockStatus.equals("con_hang")) {
                filtered = filtered.stream().filter(p -> p.getQuantityInStock() != null && p.getQuantityInStock() > 0)
                        .toList();
            } else if (stockStatus.equals("het_hang")) {
                filtered = filtered.stream().filter(p -> p.getQuantityInStock() != null && p.getQuantityInStock() == 0)
                        .toList();
            }
        }

        // Sắp xếp theo giá
        if (sort != null) {
            if (sort.equals("asc")) {
                filtered = filtered.stream().sorted((a, b) -> a.getPrice().compareTo(b.getPrice())).toList();
            } else if (sort.equals("desc")) {
                filtered = filtered.stream().sorted((a, b) -> b.getPrice().compareTo(a.getPrice())).toList();
            }
        }

        List<ProductResponse> dtoList = filtered.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, productPage.getTotalElements());
    }

    public List<ProductResponse> getTopSellingProducts(int topN) {
        List<OrderItem> allItems = orderItemRepository.findAll();
        Map<Product, Integer> productSales = new HashMap<>();
        for (OrderItem item : allItems) {
            productSales.merge(item.getProduct(), item.getQuantity(), Integer::sum);
        }
        return productSales.entrySet().stream()
                .sorted(Map.Entry.<Product, Integer>comparingByValue().reversed())
                .limit(topN)
                .map(Map.Entry::getKey)
                .map(this::convertToDto) // Convert to DTO here
                .collect(Collectors.toList());
    }

    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
        return convertToDto(product);
    }

    private CategoryResponse convertCategoryToDto(Category category) {
        if (category == null) {
            return null;
        }
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }

    private ProductImageResponse convertImageToDto(ProductImage image) {
        if (image == null) {
            return null;
        }
        return ProductImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .build();
    }

    private ProductResponse convertToDto(Product product) {
        if (product == null) {
            return null;
        }

        List<ProductImageResponse> imageResponses = product.getImages().stream()
                .map(this::convertImageToDto)
                .collect(Collectors.toList());

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .price(product.getPrice())
                .discountPrice(product.getDiscountPrice())
                .thumbnailUrl(product.getThumbnailUrl())
                .material(product.getMaterial())
                .dimensions(product.getDimensions())
                .quantityInStock(product.getQuantityInStock())
                .category(convertCategoryToDto(product.getCategory()))
                .images(imageResponses)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
