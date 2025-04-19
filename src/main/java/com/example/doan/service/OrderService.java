package com.example.doan.service;

import com.example.doan.dto.request.OrderRequest;
import com.example.doan.dto.response.OrderItemResponse;
import com.example.doan.dto.response.OrderResponse;
import com.example.doan.entity.*;
import com.example.doan.exception.AppException;
import com.example.doan.exception.ErrorCode;
import com.example.doan.repository.OrderItemRepository;
import com.example.doan.repository.OrderRepository;
import com.example.doan.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final UserRepository userRepository;

    @Transactional
    public Order placeOrder(String username, OrderRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Cart cart = cartService.getMyCart(username);
        if (cart.getItems().isEmpty()) {
            throw new AppException(ErrorCode.CART_EMPTY);
        }

        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(request.getShippingAddress());
        order.setStatus(Order.OrderStatus.PENDING);
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

        orderRepository.save(order);

        // clear cart
        cart.getItems().clear();

        return order;
    }

    public List<Order> getMyOrders(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return orderRepository.findByUser(user);
    }

    public OrderResponse convertToDto(Order order) {
        List<OrderItemResponse> items = order.getItems().stream().map(item -> {
            BigDecimal subtotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            return OrderItemResponse.builder()
                    .productId(item.getProduct().getId())
                    .productName(item.getProduct().getName())
                    .price(item.getPrice())
                    .quantity(item.getQuantity())
                    .subtotal(subtotal)
                    .build();
        }).toList();

        return OrderResponse.builder()
                .id(order.getId())
                .shippingAddress(order.getShippingAddress())
                .status(order.getStatus().name())
                .total(order.getTotal())
                .createdAt(order.getCreatedAt())
                .items(items)
                .build();
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional
    public Order updateStatus(Long id, Order.OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // Nếu đơn đã bị huỷ và người huỷ là USER thì admin không được chỉnh sửa nữa
        if ("USER".equals(order.getCancelledBy()) && order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new AppException(ErrorCode.ORDER_CANCELED_BY_USER);
        }

        order.setStatus(status);
        if (status == Order.OrderStatus.CANCELLED) {
            order.setCancelledBy("ADMIN");
        } else {
            order.setCancelledBy(null);
        }
        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrder(String username, Long orderId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUser().getUserId().equals(user.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (!order.getStatus().equals(Order.OrderStatus.PENDING)) {
            throw new AppException(ErrorCode.ORDER_CANCEL_FAILED);
        }
        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setCancelledBy("USER");
        return orderRepository.save(order);
    }

    // ✅ Lọc đơn theo trạng thái cho admin
    public List<OrderResponse> getAllOrdersByStatus(Order.OrderStatus status) {
        List<Order> orders = (status != null)
                ? orderRepository.findByStatus(status)
                : orderRepository.findAll();
        return orders.stream().map(this::convertToDto).toList();
    }

    // ✅ Lọc đơn theo trạng thái cho user
    public List<OrderResponse> getMyOrdersByStatus(String username, Order.OrderStatus status) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        List<Order> orders = (status != null)
                ? orderRepository.findByUserAndStatus(user, status)
                : orderRepository.findByUser(user);
        return orders.stream().map(this::convertToDto).toList();
    }

}