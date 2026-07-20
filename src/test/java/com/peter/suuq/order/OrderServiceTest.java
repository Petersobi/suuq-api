package com.peter.suuq.order;

import com.peter.suuq.cart.entity.Cart;
import com.peter.suuq.cart.entity.CartItem;
import com.peter.suuq.cart.service.CartService;
import com.peter.suuq.exception.BadRequestException;
import com.peter.suuq.exception.ResourceNotFoundException;
import com.peter.suuq.order.dto.OrderRequest;
import com.peter.suuq.order.dto.OrderResponse;
import com.peter.suuq.order.entity.Order;
import com.peter.suuq.order.entity.OrderItem;
import com.peter.suuq.order.entity.OrderStatus;
import com.peter.suuq.order.repository.OrderRepository;
import com.peter.suuq.order.service.OrderService;
import com.peter.suuq.product.entity.Category;
import com.peter.suuq.product.entity.Product;
import com.peter.suuq.user.entity.Role;
import com.peter.suuq.user.entity.User;
import com.peter.suuq.user.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartService cartService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private OrderService orderService;

    private User buildUser() {
        return User.builder()
                .id(1L)
                .fullName("Peter Somto")
                .email("peter@suuq.com")
                .role(Role.CUSTOMER)
                .build();
    }

    private Product buildProduct() {
        return Product.builder()
                .id(1L)
                .name("Wireless Earbuds")
                .price(new BigDecimal("15000.00"))
                .stockQuantity(50)
                .active(true)
                .category(Category.builder().id(1L).name("Electronics").build())
                .build();
    }

    private Cart buildCartWithItems(User user, Product product) {
        CartItem item = CartItem.builder()
                .id(1L)
                .product(product)
                .quantity(2)
                .build();

        Cart cart = Cart.builder()
                .id(1L)
                .user(user)
                .items(new ArrayList<>(List.of(item)))
                .build();
        item.setCart(cart);
        return cart;
    }

    @Test
    void placeOrder_shouldReturnOrderResponse_whenCartHasItems() {
        // Arrange
        User user = buildUser();
        Product product = buildProduct();
        Cart cart = buildCartWithItems(user, product);

        OrderRequest request = new OrderRequest();
        request.setShippingAddress("14 Awolowo Road, Lagos");

        when(cartService.getCartEntity(user)).thenReturn(cart);
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    order.setId(1L);
                    order.setCreatedAt(LocalDateTime.now());
                    return order;
                });
        doNothing().when(cartService).clearCart(cart);
        doNothing().when(emailService).sendOrderConfirmationEmail(
                any(), any(), any(), any());

        // Act
        OrderResponse response = orderService.placeOrder(user, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getTotalAmount()).isEqualTo(new BigDecimal("30000.00"));
        assertThat(response.getShippingAddress()).isEqualTo("14 Awolowo Road, Lagos");
        assertThat(response.getItems()).hasSize(1);

        verify(orderRepository).save(any(Order.class));
        verify(cartService).clearCart(cart);
        verify(emailService).sendOrderConfirmationEmail(
                eq("peter@suuq.com"), eq("Peter Somto"), any(), any());
    }

    @Test
    void placeOrder_shouldThrow_whenCartIsEmpty() {
        // Arrange
        User user = buildUser();
        Cart emptyCart = Cart.builder()
                .id(1L)
                .user(user)
                .items(new ArrayList<>())
                .build();

        when(cartService.getCartEntity(user)).thenReturn(emptyCart);

        // Act & Assert
        assertThatThrownBy(() -> orderService.placeOrder(user, new OrderRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot place order with empty cart");

        verify(orderRepository, never()).save(any());
        verify(cartService, never()).clearCart(any());
    }

    @Test
    void getOrderById_shouldThrow_whenOrderBelongsToDifferentUser() {
        // Arrange
        User user = buildUser();

        User otherUser = User.builder()
                .id(2L)
                .email("other@suuq.com")
                .build();

        Order order = Order.builder()
                .id(1L)
                .user(otherUser)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("30000.00"))
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThatThrownBy(() -> orderService.getOrderById(user, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Access denied");
    }

    @Test
    void getOrderById_shouldThrow_whenOrderNotFound() {
        User user = buildUser();
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(user, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found");
    }

    @Test
    void updateOrderStatus_shouldUpdateStatus_whenOrderExists() {
        // Arrange
        Order order = Order.builder()
                .id(1L)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("30000.00"))
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderResponse response = orderService.updateOrderStatus(1L, OrderStatus.PROCESSING);

        // Assert
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        verify(orderRepository).save(order);
    }
}