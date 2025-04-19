package com.example.doan.service;

import com.example.doan.dto.request.CartItemRequest;
import com.example.doan.dto.response.CartItemResponse;
import com.example.doan.dto.response.CartResponse;
import com.example.doan.entity.Cart;
import com.example.doan.entity.CartItem;
import com.example.doan.entity.Product;
import com.example.doan.entity.User;
import com.example.doan.exception.AppException;
import com.example.doan.exception.ErrorCode;
import com.example.doan.repository.CartItemRepository;
import com.example.doan.repository.CartRepository;
import com.example.doan.repository.ProductRepository;
import com.example.doan.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@RequiredArgsConstructor
@Service
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public Cart getMyCart(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    return cartRepository.save(cart);
                });
    }

    public Cart addToCart(String username, CartItemRequest request) {
        if (request.getQuantity() <= 0) {
            throw new AppException(ErrorCode.PRODUCT_LARGE_THAN_0);
        }
        Cart cart = getMyCart(username);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        CartItem item = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseGet(() -> new CartItem(null, cart, product, 0));

        item.setQuantity(item.getQuantity() + request.getQuantity());
        cartItemRepository.save(item);

        return getMyCart(username);
    }

    public Cart updateCartItem(String username, CartItemRequest request) {
        Cart cart = getMyCart(username);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        CartItem item = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        if (request.getQuantity() <= 0) {
            cartItemRepository.delete(item); // ✅ Xoá khi số lượng = 0
        } else {
            item.setQuantity(request.getQuantity());
            cartItemRepository.save(item);
        }

        return getMyCart(username);
    }

    @Transactional
    public void removeFromCart(String username, Long productId) {
        Cart cart = getMyCart(username);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        cartItemRepository.deleteByCartAndProduct(cart, product);
    }

    public CartResponse convertToResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream().map(item -> {
            var product = item.getProduct();
            BigDecimal price = product.getDiscountPrice() != null ? product.getDiscountPrice() : product.getPrice();
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(item.getQuantity()));

            return CartItemResponse.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .thumbnailUrl(product.getThumbnailUrl())
                    .price(price)
                    .quantity(item.getQuantity())
                    .subtotal(subtotal)
                    .build();
        }).toList();

        BigDecimal total = items.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .items(items)
                .total(total)
                .build();
    }
}
