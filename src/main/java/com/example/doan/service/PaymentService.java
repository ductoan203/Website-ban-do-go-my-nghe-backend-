package com.example.doan.service;

import com.example.doan.dto.request.OrderRequest;
import com.example.doan.entity.*;
import com.example.doan.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final UserRepository userRepository;
    private final CartService cartService;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional
    public Order handleCheckout(String username, OrderRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Cart cart = cartService.getMyCart(username);

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(request.getShippingAddress());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setCreatedAt(Instant.now());

        List<OrderItem> orderItems = cart.getItems().stream().map(cartItem -> {
            Product product = cartItem.getProduct();
            BigDecimal price = product.getDiscountPrice() != null ? product.getDiscountPrice() : product.getPrice();

            return new OrderItem(null, order, product, cartItem.getQuantity(), price);
        }).toList();

        BigDecimal total = orderItems.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setItems(orderItems);
        order.setTotal(total);

        if ("COD".equalsIgnoreCase(request.getPaymentMethod())) {
            order.setStatus(Order.OrderStatus.CONFIRMED);
            order.setPaymentStatus("PAID");
        } else {
            order.setStatus(Order.OrderStatus.PENDING);
            order.setPaymentStatus("UNPAID");
        }

        orderRepository.save(order);
        cart.getItems().clear();
        return order;
    }
}
