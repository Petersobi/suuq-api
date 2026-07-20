package com.peter.suuq.cart;

import com.peter.suuq.cart.dto.CartItemRequest;
import com.peter.suuq.cart.dto.CartResponse;
import com.peter.suuq.cart.entity.Cart;
import com.peter.suuq.cart.entity.CartItem;
import com.peter.suuq.cart.repository.CartItemRepository;
import com.peter.suuq.cart.repository.CartRepository;
import com.peter.suuq.cart.service.CartService;
import com.peter.suuq.exception.BadRequestException;
import com.peter.suuq.exception.ResourceNotFoundException;
import com.peter.suuq.product.entity.Category;
import com.peter.suuq.product.entity.Product;
import com.peter.suuq.product.repository.ProductRepository;
import com.peter.suuq.user.entity.Role;
import com.peter.suuq.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartService cartService;

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

    @Test
    void addToCart_shouldCreateNewCart_whenUserHasNoCart() {
        // Arrange
        User user = buildUser();
        Product product = buildProduct();

        CartItemRequest request = new CartItemRequest();
        request.setProductId(1L);
        request.setQuantity(2);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class)))
                .thenAnswer(invocation -> {
                    Cart cart = invocation.getArgument(0);
                    cart.setId(1L);
                    if (cart.getItems() == null) {
                        cart.setItems(new ArrayList<>());
                    }
                    return cart;
                });
        when(cartItemRepository.findByCartIdAndProductId(any(), any()))
                .thenReturn(Optional.empty());

        // Act
        CartResponse response = cartService.addToCart(user, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(response.getTotal()).isEqualTo(new BigDecimal("30000.00"));

        verify(cartRepository, times(2)).save(any(Cart.class));
    }

    @Test
    void addToCart_shouldThrow_whenProductIsInactive() {
        // Arrange
        User user = buildUser();
        Product product = buildProduct();
        product.setActive(false);

        CartItemRequest request = new CartItemRequest();
        request.setProductId(1L);
        request.setQuantity(2);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // Act & Assert
        assertThatThrownBy(() -> cartService.addToCart(user, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Product is no longer available");
    }

    @Test
    void addToCart_shouldThrow_whenInsufficientStock() {
        // Arrange
        User user = buildUser();
        Product product = buildProduct();
        product.setStockQuantity(1);

        CartItemRequest request = new CartItemRequest();
        request.setProductId(1L);
        request.setQuantity(5);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // Act & Assert
        assertThatThrownBy(() -> cartService.addToCart(user, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Insufficient stock");
    }

    @Test
    void removeFromCart_shouldThrow_whenCartNotFound() {
        User user = buildUser();

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeFromCart(user, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Cart not found");
    }

    @Test
    void updateCartItem_shouldRemoveItem_whenQuantityIsZero() {
        // Arrange
        User user = buildUser();
        Product product = buildProduct();

        CartItem item = CartItem.builder()
                .id(1L)
                .product(product)
                .quantity(2)
                .build();

        Cart cart = Cart.builder()
                .id(1L)
                .user(user)
                .items(new ArrayList<>(java.util.List.of(item)))
                .build();
        item.setCart(cart);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CartResponse response = cartService.updateCartItem(user, 1L, 0);

        // Assert
        assertThat(cart.getItems()).isEmpty();
        assertThat(response.getTotal()).isEqualTo(BigDecimal.ZERO);
    }
}