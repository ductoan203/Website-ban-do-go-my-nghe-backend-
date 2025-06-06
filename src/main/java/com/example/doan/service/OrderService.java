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
import com.example.doan.util.SecurityUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    public Order findByOrderId(String orderId) {
        return orderRepository.findById(Long.parseLong(orderId)) // ✅ dùng id
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
    }

    @Transactional
    public Order placeOrder(String username, OrderRequest request) {
        log.info("[ORDER] === BẮT ĐẦU PLACE ORDER ===");
        Long userId = SecurityUtil.getCurrentUserId();
        log.info("[ORDER] userId from token: {}", userId);
        User user = null;
        Cart cart;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            log.info("[ORDER] user from DB: {}", user);
            cart = cartService.getMyCart(user);
            log.info("[ORDER] cart for user: {}", cart);
            if (cart.getItems().isEmpty()) {
                log.info("[ORDER] Cart is empty for user {}");
                throw new AppException(ErrorCode.CART_EMPTY);
            }
        } else {
            // Khách vãng lai: tạo cart tạm từ request.items
            cart = cartService.getCartFromRequest(request);
            log.info("[ORDER] guest cart: {}", cart);
            if (cart.getItems().isEmpty()) {
                log.info("[ORDER] Guest cart is empty");
                throw new AppException(ErrorCode.CART_EMPTY);
            }
        }

        Order order = new Order();
        if (user != null) {
            order.setUser(user);
            log.info("[ORDER] set user for order: {}", user.getUserId());
        }
        order.setShippingAddress(request.getShippingAddress());
        order.setStatus(Order.OrderStatus.PENDING);
        order.setCreatedAt(Instant.now());
        order.setCustomerName(request.getCustomerName());
        order.setEmail(request.getEmail());
        order.setPhone(request.getPhone());

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
        log.info("[ORDER] order after save: {}", order);

        // clear cart nếu là user đăng nhập
        if (user != null) {
            cart.getItems().clear();
        }

        log.info("[ORDER] === KẾT THÚC PLACE ORDER ===");
        return order;
    }

    public List<Order> getMyOrders() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null)
            throw new AppException(ErrorCode.UNAUTHORIZED);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return orderRepository.findByUser(user);
    }

    public OrderResponse convertToDto(Order order) {
        List<OrderItemResponse> items = order.getItems().stream().map(item -> {
            BigDecimal subtotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            return OrderItemResponse.builder()
                    .productId(item.getProduct().getId())
                    .productName(item.getProduct().getName())
                    .imageUrl(item.getProduct().getThumbnailUrl() != null
                            ? "http://localhost:8080/doan" + item.getProduct().getThumbnailUrl()
                            : null)
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
                .customerName(order.getUser() != null ? order.getUser().getFullname() : order.getCustomerName())
                .email(order.getEmail())
                .phone(order.getPhone())
                .createdAt(order.getCreatedAt())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
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
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null)
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // Đảm bảo người dùng đang đăng nhập là chủ đơn hàng
        if (!order.getUser().getUserId().equals(user.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Chỉ cho phép hủy nếu trạng thái chưa phải ĐÃ GIAO, ĐÃ HỦY hoặc ĐANG GIAO
        if (order.getStatus() == Order.OrderStatus.DELIVERED || order.getStatus() == Order.OrderStatus.CANCELLED
                || order.getStatus() == Order.OrderStatus.SHIPPED) {
            throw new AppException(ErrorCode.ORDER_CANCEL_FAILED); // hoặc một mã lỗi phù hợp hơn
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
                .orElse(userRepository.findByEmail(username)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND)));
        List<Order> orders = (status != null)
                ? orderRepository.findByUserAndStatus(user, status)
                : orderRepository.findByUser(user);
        return orders.stream().map(this::convertToDto).toList();
    }

    public Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
    }

    public Order save(Order order) {
        return orderRepository.save(order);
    }

    @Transactional
    public void deleteOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        orderRepository.delete(order);
    }

}