package com.example.doan.service;

import com.example.doan.dto.request.OrderRequest;
import com.example.doan.entity.*;
import com.example.doan.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final UserRepository userRepository;
    private final CartService cartService;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final EmailService emailService;

    @Transactional
    public Order handleCheckout(String username, OrderRequest request) {
        logger.info("[PAYMENT] === BẮT ĐẦU HANDLE CHECKOUT ===");
        logger.info("[PAYMENT] username: {}", username);
        Cart cart;
        Order order = new Order();
        User user = null;

        if (username != null && !username.equals("anonymousUser")) {
            Optional<User> optionalUser = userRepository.findByUsername(username);
            if (optionalUser.isEmpty()) {
                // Thử tìm theo email nếu username là email
                optionalUser = userRepository.findByEmail(username);
                if (optionalUser.isPresent()) {
                    logger.info("[PAYMENT] Tìm thấy user theo email: {} (userId: {})", username,
                            optionalUser.get().getUserId());
                } else {
                    logger.info("[PAYMENT] Không tìm thấy user với username/email: {}", username);
                }
            } else {
                logger.info("[PAYMENT] Tìm thấy user theo username: {} (userId: {})", username,
                        optionalUser.get().getUserId());
            }
            if (optionalUser.isPresent()) {
                user = optionalUser.get();
                order.setUser(user);
                cart = cartService.getMyCart(user);
                logger.info("[PAYMENT] cart for userId: {}", user.getUserId());
            } else {
                cart = cartService.getCartFromRequest(request); // fallback nếu username/email rác
            }
        } else {
            // Người dùng chưa đăng nhập → lấy giỏ hàng từ request
            logger.info("[PAYMENT] Khách vãng lai, tạo cart từ request.items");
            cart = cartService.getCartFromRequest(request);
            cart.setItems(new ArrayList<>(
                    request.getItems().stream().map(item -> {
                        Product product = productRepository.findById(item.getProductId())
                                .orElseThrow(() -> new RuntimeException("Product not found"));
                        CartItem cartItem = new CartItem();
                        cartItem.setProduct(product);
                        cartItem.setQuantity(item.getQuantity());
                        return cartItem;
                    }).toList()));
        }

        if (cart.getItems().isEmpty()) {
            logger.info("[PAYMENT] Cart is empty!");
            throw new RuntimeException("Cart is empty");
        }

        order.setShippingAddress(request.getShippingAddress());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setCreatedAt(Instant.now());
        order.setCustomerName(request.getCustomerName());
        order.setEmail(request.getEmail());
        order.setPhone(request.getPhone());

        List<OrderItem> orderItems = cart.getItems().stream().map(cartItem -> {
            Product product = cartItem.getProduct();
            BigDecimal price = product.getDiscountPrice();
            if (price == null || price.compareTo(BigDecimal.ZERO) == 0) {
                price = product.getPrice();
            }
            if (price == null) {
                throw new RuntimeException("Sản phẩm không có giá");
            }
            OrderItem orderItem = new OrderItem(null, null, product, cartItem.getQuantity(), price);
            orderItem.setOrder(order);
            return orderItem;
        }).toList();

        BigDecimal total = orderItems.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setItems(orderItems);
        order.setTotal(total);

        logger.info("[PAYMENT] orderId (chưa lưu): null, userId: {}", user != null ? user.getUserId() : null);

        if ("COD".equalsIgnoreCase(request.getPaymentMethod())) {
            order.setStatus(Order.OrderStatus.CONFIRMED);
            order.setPaymentStatus("UNPAID");
            order.setPaymentMethod("COD");
            Order savedOrder = orderRepository.save(order);

            // Trừ số lượng tồn kho cho từng sản phẩm trong đơn hàng
            for (OrderItem item : savedOrder.getItems()) {
                Product product = item.getProduct();
                int newQuantityInStock = product.getQuantityInStock() - item.getQuantity();
                if (newQuantityInStock < 0) {
                    logger.warn("[PAYMENT] Tồn kho âm cho sản phẩm {} (id: {}). New stock: {}", product.getName(),
                            product.getId(), newQuantityInStock);
                    newQuantityInStock = 0;
                }
                product.setQuantityInStock(newQuantityInStock);
                productRepository.save(product);
                logger.info("[PAYMENT] Đã trừ tồn kho cho sản phẩm {} (id: {}). Tồn kho mới: {}", product.getName(),
                        product.getId(), newQuantityInStock);
            }

            // Gửi email xác nhận cho COD
            emailService.sendOrderConfirmationEmail(
                    order.getEmail(),
                    order.getCustomerName(),
                    order.getId().toString(),
                    order.getTotal(),
                    order.getPaymentMethod(),
                    order.getShippingAddress());

            return savedOrder;
        } else if ("PAYOS".equalsIgnoreCase(request.getPaymentMethod())) {
            // Đơn hàng PayOS: chỉ lưu với trạng thái PENDING, chưa trừ tồn kho
            order.setStatus(Order.OrderStatus.PENDING);
            order.setPaymentStatus("PENDING");
            order.setPaymentMethod("PAYOS");
            return orderRepository.save(order);
        } else {
            // Đơn online: chờ thanh toán thành công mới xác nhận
            order.setStatus(Order.OrderStatus.PENDING);
            order.setPaymentStatus("PENDING");
            order.setPaymentMethod(request.getPaymentMethod());
        }
        Order savedOrder = orderRepository.save(order);

        // Gán id cho orderRequest nếu có (để truyền cho VNPAY)
        request.setId(savedOrder.getId());

        logger.info("[PAYMENT] orderId: {}, userId trong order: {}", savedOrder.getId(),
                savedOrder.getUser() != null ? savedOrder.getUser().getUserId() : null);
        logger.info("[PAYMENT] === KẾT THÚC HANDLE CHECKOUT ===");
        return savedOrder;
    }

    @Scheduled(fixedRate = 300000) // Chạy mỗi 5 phút
    @Transactional
    public void cleanupPendingOnlineOrders() {
        try {
            Instant expired = Instant.now().minusSeconds(1800); // 30 phút
            List<Order> pendingOrders = orderRepository.findByStatusAndCreatedAtBefore(Order.OrderStatus.PENDING,
                    expired);
            if (!pendingOrders.isEmpty()) {
                logger.info("Cleaning up {} pending online orders (VNPAY, PAYOS, MOMO)", pendingOrders.size());
                orderRepository.deleteAll(pendingOrders);
            }
        } catch (Exception e) {
            logger.error("Error cleaning up pending online orders", e);
        }
    }

}