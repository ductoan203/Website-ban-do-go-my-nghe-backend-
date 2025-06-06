package com.example.doan.service;

import com.example.doan.dto.request.CartItemRequest;
import com.example.doan.dto.request.OrderRequest;
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
import com.example.doan.util.SecurityUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public Cart getMyCart(User user) {
        if (user == null)
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    return cartRepository.save(cart);
                });
    }

    public User getCurrentUser() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null)
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    public Cart addToCart(CartItemRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null)
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (request.getQuantity() <= 0) {
            throw new AppException(ErrorCode.PRODUCT_LARGE_THAN_0);
        }

        Cart cart = getMyCart(user);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        CartItem item = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setCart(cart);
                    newItem.setProduct(product);
                    newItem.setQuantity(0);
                    newItem.setPrice(product.getPrice());
                    return newItem;
                });

        int currentQuantity = item.getQuantity();
        int addedQuantity = request.getQuantity();
        int totalQuantity = currentQuantity + addedQuantity;

        // Kiểm tra số lượng tồn kho
        if (totalQuantity > product.getQuantityInStock()) {
            throw new AppException(ErrorCode.PRODUCT_QUANTITY_EXCEEDED);
        }

        item.setQuantity(totalQuantity);
        item.setPrice(product.getPrice());
        cartItemRepository.save(item);

        return getMyCart(user);
    }

    public Cart updateCartItem(CartItemRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null)
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Cart cart = getMyCart(user);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        CartItem item = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        if (request.getQuantity() <= 0) {
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(request.getQuantity());
            cartItemRepository.save(item);
        }

        return getMyCart(user);
    }

    @Transactional
    public void removeFromCart(Long productId) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null)
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Cart cart = getMyCart(user);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        cartItemRepository.deleteByCartAndProduct(cart, product);
    }

    public CartResponse convertToResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream().map(item -> {
            var product = item.getProduct();

            // ✅ Ưu tiên dùng item.getPrice() vì đó là giá đã lưu
            BigDecimal price = item.getPrice(); // <-- Sửa tại đây!
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

    public Cart getCartFromRequest(OrderRequest request) {
        Cart guestCart = new Cart();
        List<CartItem> items = new ArrayList<>();

        for (CartItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(
                            () -> new RuntimeException("Sản phẩm không tồn tại với id: " + itemRequest.getProductId()));
            CartItem cartItem = new CartItem();
            cartItem.setProduct(product);
            cartItem.setQuantity(itemRequest.getQuantity());
            cartItem.setCart(guestCart);
            cartItem.setPrice(product.getPrice()); // ✅ đảm bảo lấy đúng giá sản phẩm
            items.add(cartItem);
        }

        guestCart.setItems(items);
        return guestCart;
    }
}
