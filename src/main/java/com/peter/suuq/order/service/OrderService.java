package com.peter.suuq.order.service;

import com.peter.suuq.cart.entity.Cart;
import com.peter.suuq.cart.entity.CartItem;
import com.peter.suuq.cart.service.CartService;
import com.peter.suuq.exception.AccessDeniedException;
import com.peter.suuq.exception.BadRequestException;
import com.peter.suuq.exception.ResourceNotFoundException;
import com.peter.suuq.order.dto.OrderRequest;
import com.peter.suuq.order.dto.OrderItemResponse;
import com.peter.suuq.order.dto.OrderResponse;
import com.peter.suuq.order.entity.Order;
import com.peter.suuq.order.entity.OrderItem;
import com.peter.suuq.order.entity.OrderStatus;
import com.peter.suuq.order.repository.OrderRepository;
import com.peter.suuq.user.entity.User;
import com.peter.suuq.user.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final EmailService emailService;

    @Transactional
    public OrderResponse placeOrder(User user, OrderRequest request) {
        Cart cart = cartService.getCartEntity(user);

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot place order with empty cart");
        }

        List<OrderItem> orderItems = cart.getItems().stream()
                .map(cartItem -> OrderItem.builder()
                        .product(cartItem.getProduct())
                        .quantity(cartItem.getQuantity())
                        .unitPrice(cartItem.getProduct().getPrice())
                        .build())
                .toList();

        BigDecimal total = orderItems.stream()
                .map(item -> item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .totalAmount(total)
                .shippingAddress(request.getShippingAddress())
                .build();

        orderItems.forEach(item -> item.setOrder(order));
        order.setItems(orderItems);

        orderRepository.save(order);
        cartService.clearCart(cart);

        emailService.sendOrderConfirmationEmail(
                user.getEmail(),
                user.getFullName(),
                order.getId(),
                order.getTotalAmount().toString()
        );

        return mapToResponse(order);

    }

    public List<OrderResponse> getMyOrders(User user) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public OrderResponse getOrderById(User user, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied");
        }

        return mapToResponse(order);
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        order.setStatus(status);
        return mapToResponse(orderRepository.save(order));
    }

    @Transactional
    public void updateOrderAfterPayment(String paystackReference) {
        Order order = orderRepository.findByPaystackReference(paystackReference)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
    }

    public Order getOrderEntityByReference(String reference) {
        return orderRepository.findByPaystackReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    // ---------- Mapper ----------

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getUnitPrice()
                                .multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .toList();

        return OrderResponse.builder()
                .orderId(order.getId())
                .items(itemResponses)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .shippingAddress(order.getShippingAddress())
                .paystackReference(order.getPaystackReference())
                .createdAt(order.getCreatedAt())
                .build();
    }
}