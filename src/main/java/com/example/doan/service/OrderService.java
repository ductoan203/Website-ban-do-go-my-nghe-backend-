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
import com.example.doan.repository.ProductRepository;
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
    private final ProductRepository productRepository;
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
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentStatus(request.getPaymentStatus());

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
                .cancelledBy(order.getCancelledBy())
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
    public Order updateStatus(Long id, Order.OrderStatus newStatus) {
        log.info("[ORDER_STATUS] Bắt đầu cập nhật trạng thái đơn hàng. Order ID: {}, Trạng thái mới: {}", id,
                newStatus);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[ORDER_STATUS] Không tìm thấy đơn hàng với ID: {}", id);
                    return new AppException(ErrorCode.ORDER_NOT_FOUND);
                });

        // Lấy trạng thái hiện tại của đơn hàng
        Order.OrderStatus currentStatus = order.getStatus();
        String cancelledBy = order.getCancelledBy(); // Lấy thông tin người hủy
        log.info("[ORDER_STATUS] Trạng thái hiện tại của đơn hàng ID {}: {}, Người hủy: {}", id, currentStatus,
                cancelledBy);

        // Kiểm tra xem chuyển đổi trạng thái có hợp lệ không, truyền thêm thông tin
        // người hủy
        if (!isValidStatusTransition(currentStatus, newStatus, cancelledBy)) {
            log.warn("[ORDER_STATUS] Chuyển đổi trạng thái không hợp lệ: Từ {} sang {}. Người hủy: {}. Order ID: {}",
                    currentStatus, newStatus, cancelledBy, id);
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS_TRANSITION);
        }

        order.setStatus(newStatus);

        // Cập nhật trường cancelledBy khi chuyển trạng thái thành CANCELLED
        if (newStatus == Order.OrderStatus.CANCELLED) {
            // Nếu trạng thái mới là CANCELLED và đơn hàng chưa bị USER hủy
            if (!"USER".equals(order.getCancelledBy())) {
                order.setCancelledBy("ADMIN");
            }
        } else if (currentStatus == Order.OrderStatus.CANCELLED && !newStatus.equals(Order.OrderStatus.CANCELLED)) {
            // Nếu trạng thái cũ là CANCELLED và trạng thái mới không phải CANCELLED (tức là
            // hoàn tác)
            order.setCancelledBy(null); // Xóa người hủy khi hoàn tác
        }

        Order updatedOrder = orderRepository.save(order);
        log.info("[ORDER_STATUS] Cập nhật trạng thái đơn hàng thành công. Order ID: {}, Trạng thái mới: {}", id,
                updatedOrder.getStatus());
        return updatedOrder;
    }

    private boolean isValidStatusTransition(Order.OrderStatus currentStatus, Order.OrderStatus newStatus,
                                            String cancelledBy) {
        log.info("[ORDER_STATUS_TRANSITION] Kiểm tra chuyển đổi từ {} sang {}. Người hủy (nếu có): {}", currentStatus,
                newStatus, cancelledBy);
        // Nếu đơn hàng đang ở trạng thái CANCELLED hoặc RETURNED
        if (currentStatus == Order.OrderStatus.CANCELLED || currentStatus == Order.OrderStatus.RETURNED) {
            // Admin có thể hoàn tác đơn hàng nếu nó bị hủy bởi ADMIN
            if (currentStatus == Order.OrderStatus.CANCELLED && "ADMIN".equals(cancelledBy)) {
                // Admin có thể chuyển từ CANCELLED trở lại PENDING hoặc CONFIRMED
                return newStatus == Order.OrderStatus.PENDING || newStatus == Order.OrderStatus.CONFIRMED;
            }
            log.info("[ORDER_STATUS_TRANSITION] Không thể chuyển đổi từ trạng thái cuối hoặc bị hủy bởi USER: {}",
                    currentStatus);
            return false; // Không thể chuyển đổi từ các trạng thái cuối này (hoặc nếu bị user hủy)
        }

        switch (currentStatus) {
            case PENDING:
                return newStatus == Order.OrderStatus.CONFIRMED || newStatus == Order.OrderStatus.CANCELLED;
            case CONFIRMED:
                return newStatus == Order.OrderStatus.READY_FOR_DELIVERY || newStatus == Order.OrderStatus.CANCELLED;
            case READY_FOR_DELIVERY:
                return newStatus == Order.OrderStatus.SHIPPED || newStatus == Order.OrderStatus.CANCELLED;
            case SHIPPED:
                return newStatus == Order.OrderStatus.DELIVERED || newStatus == Order.OrderStatus.RETURNED
                        || newStatus == Order.OrderStatus.CANCELLED;
            case DELIVERED:
                return newStatus == Order.OrderStatus.RETURNED; // Có thể trả hàng sau khi đã giao
            default:
                log.warn("[ORDER_STATUS_TRANSITION] Trạng thái hiện tại không xác định: {}", currentStatus);
                return false;
        }
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

        // Chỉ cho phép hủy nếu trạng thái chưa phải ĐÃ GIAO, ĐÃ HỦY hoặc ĐANG GIAO HOẶC
        // ĐÃ TRẢ HÀNG
        if (order.getStatus() == Order.OrderStatus.DELIVERED
                || order.getStatus() == Order.OrderStatus.CANCELLED
                || order.getStatus() == Order.OrderStatus.SHIPPED
                || order.getStatus() == Order.OrderStatus.RETURNED) {
            throw new AppException(ErrorCode.ORDER_CANCEL_FAILED); // hoặc một mã lỗi phù hợp hơn
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setCancelledBy("USER");

        // Hoàn trả tồn kho cho các sản phẩm trong đơn hàng bị hủy
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setQuantityInStock(product.getQuantityInStock() + item.getQuantity());
            productRepository.save(product);
            log.info("[ORDER] Đã hoàn trả tồn kho cho sản phẩm {} (id: {}). Tồn kho mới: {}", product.getName(),
                    product.getId(), product.getQuantityInStock());
        }

        return orderRepository.save(order);
    }

    @Transactional
    public Order returnOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // Chỉ cho phép trả hàng nếu trạng thái là ĐÃ GIAO
        if (order.getStatus() != Order.OrderStatus.DELIVERED) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS_TRANSITION); // Sử dụng lỗi chung hoặc tạo lỗi mới
        }

        order.setStatus(Order.OrderStatus.RETURNED);

        // Hoàn trả tồn kho cho các sản phẩm trong đơn hàng được trả
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setQuantityInStock(product.getQuantityInStock() + item.getQuantity());
            productRepository.save(product);
            log.info("[ORDER] Đã hoàn trả tồn kho cho sản phẩm {} (id: {}). Tồn kho mới: {}", product.getName(),
                    product.getId(), product.getQuantityInStock());
        }
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
        log.info("[OrderService] Retrieved {} orders for user: {}", orders.size(), username);
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

    @Transactional
    public void deductStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            int newQuantityInStock = product.getQuantityInStock() - item.getQuantity();
            if (newQuantityInStock < 0) {
                log.warn("[ORDER] Tồn kho âm cho sản phẩm {} (id: {}). New stock: {}", product.getName(),
                        product.getId(), newQuantityInStock);
                newQuantityInStock = 0; // Đảm bảo tồn kho không âm
            }
            product.setQuantityInStock(newQuantityInStock);
            productRepository.save(product);
            log.info("[ORDER] Đã trừ tồn kho cho sản phẩm {} (id: {}). Tồn kho mới: {}", product.getName(),
                    product.getId(), newQuantityInStock);
        }
    }

}