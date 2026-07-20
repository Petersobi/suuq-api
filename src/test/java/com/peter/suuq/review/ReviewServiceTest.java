package com.peter.suuq.review;

import com.peter.suuq.exception.BadRequestException;
import com.peter.suuq.exception.AccessDeniedException;
import com.peter.suuq.exception.ResourceNotFoundException;
import com.peter.suuq.order.entity.Order;
import com.peter.suuq.order.entity.OrderItem;
import com.peter.suuq.order.entity.OrderStatus;
import com.peter.suuq.order.repository.OrderRepository;
import com.peter.suuq.product.entity.Category;
import com.peter.suuq.product.entity.Product;
import com.peter.suuq.product.repository.ProductRepository;
import com.peter.suuq.review.dto.ProductRatingResponse;
import com.peter.suuq.review.dto.ReviewRequest;
import com.peter.suuq.review.dto.ReviewResponse;
import com.peter.suuq.review.entity.Review;
import com.peter.suuq.review.repository.ReviewRepository;
import com.peter.suuq.review.service.ReviewService;
import com.peter.suuq.user.entity.Role;
import com.peter.suuq.user.entity.User;
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
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User buildCustomer() {
        return User.builder()
                .id(1L)
                .fullName("Peter Somto")
                .email("peter@suuq.com")
                .role(Role.CUSTOMER)
                .build();
    }

    private User buildAdmin() {
        return User.builder()
                .id(2L)
                .fullName("Suuq Admin")
                .email("admin@suuq.com")
                .role(Role.ADMIN)
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

    private Order buildPaidOrder(User user, Product product) {
        OrderItem item = OrderItem.builder()
                .id(1L)
                .product(product)
                .quantity(2)
                .unitPrice(product.getPrice())
                .build();

        Order order = Order.builder()
                .id(1L)
                .user(user)
                .status(OrderStatus.PAID)
                .totalAmount(new BigDecimal("30000.00"))
                .createdAt(LocalDateTime.now())
                .items(new ArrayList<>(List.of(item)))
                .build();
        item.setOrder(order);
        return order;
    }

    @Test
    void createReview_shouldReturnReview_whenUserHasPurchasedProduct() {
        // Arrange
        User user = buildCustomer();
        Product product = buildProduct();
        Order order = buildPaidOrder(user, product);

        ReviewRequest request = new ReviewRequest();
        request.setRating(5);
        request.setComment("Excellent product!");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(order));
        when(reviewRepository.existsByUserIdAndProductId(1L, 1L))
                .thenReturn(false);
        when(reviewRepository.save(any(Review.class)))
                .thenAnswer(invocation -> {
                    Review review = invocation.getArgument(0);
                    review.setId(1L);
                    review.setCreatedAt(LocalDateTime.now());
                    return review;
                });

        // Act
        ReviewResponse response = reviewService.createReview(user, 1L, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getComment()).isEqualTo("Excellent product!");
        assertThat(response.getReviewerName()).isEqualTo("Peter Somto");

        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void createReview_shouldThrow_whenUserHasNotPurchasedProduct() {
        // Arrange
        User user = buildCustomer();
        Product product = buildProduct();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of());

        // Act & Assert
        assertThatThrownBy(() -> reviewService.createReview(
                user, 1L, new ReviewRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("You can only review products you have purchased");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_shouldThrow_whenUserAlreadyReviewed() {
        // Arrange
        User user = buildCustomer();
        Product product = buildProduct();
        Order order = buildPaidOrder(user, product);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(order));
        when(reviewRepository.existsByUserIdAndProductId(1L, 1L))
                .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> reviewService.createReview(
                user, 1L, new ReviewRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("You have already reviewed this product");
    }

    @Test
    void deleteReview_shouldDelete_whenUserIsAuthor() {
        // Arrange
        User user = buildCustomer();
        Product product = buildProduct();

        Review review = Review.builder()
                .id(1L)
                .user(user)
                .product(product)
                .rating(5)
                .build();

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        // Act
        reviewService.deleteReview(user, 1L);

        // Assert
        verify(reviewRepository).delete(review);
    }

    @Test
    void deleteReview_shouldDelete_whenUserIsAdmin() {
        // Arrange
        User admin = buildAdmin();
        User customer = buildCustomer();
        Product product = buildProduct();

        Review review = Review.builder()
                .id(1L)
                .user(customer)
                .product(product)
                .rating(5)
                .build();

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        // Act
        reviewService.deleteReview(admin, 1L);

        // Assert
        verify(reviewRepository).delete(review);
    }

    @Test
    void deleteReview_shouldThrow_whenUserIsNeitherAuthorNorAdmin() {
        // Arrange
        User randomUser = User.builder()
                .id(3L)
                .email("random@suuq.com")
                .role(Role.CUSTOMER)
                .build();

        User author = buildCustomer();
        Product product = buildProduct();

        Review review = Review.builder()
                .id(1L)
                .user(author)
                .product(product)
                .rating(5)
                .build();

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        // Act & Assert
        assertThatThrownBy(() -> reviewService.deleteReview(randomUser, 1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You do not have permission to delete this review");

        verify(reviewRepository, never()).delete(any());
    }

    @Test
    void getProductRating_shouldReturnRating_whenProductExists() {
        // Arrange
        Product product = buildProduct();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(reviewRepository.findAverageRatingByProductId(1L)).thenReturn(4.333);
        when(reviewRepository.countByProductId(1L)).thenReturn(3L);

        // Act
        ProductRatingResponse response = reviewService.getProductRating(1L);

        // Assert
        assertThat(response.getAverageRating()).isEqualTo(4.3);
        assertThat(response.getReviewCount()).isEqualTo(3L);
        assertThat(response.getProductName()).isEqualTo("Wireless Earbuds");
    }
}