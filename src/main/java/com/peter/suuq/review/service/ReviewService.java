package com.peter.suuq.review.service;

import com.peter.suuq.exception.BadRequestException;
import com.peter.suuq.exception.AccessDeniedException;
import com.peter.suuq.exception.ResourceNotFoundException;
import com.peter.suuq.order.entity.OrderStatus;
import com.peter.suuq.order.repository.OrderRepository;
import com.peter.suuq.product.entity.Product;
import com.peter.suuq.product.repository.ProductRepository;
import com.peter.suuq.review.dto.ProductRatingResponse;
import com.peter.suuq.review.dto.ReviewRequest;
import com.peter.suuq.review.dto.ReviewResponse;
import com.peter.suuq.review.entity.Review;
import com.peter.suuq.review.repository.ReviewRepository;
import com.peter.suuq.user.entity.User;
import com.peter.suuq.user.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public ReviewResponse createReview(User user, Long productId,
                                       ReviewRequest request) {

        // Check product exists
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Check customer actually purchased and paid for this product
        boolean hasPurchased = orderRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .filter(order -> order.getStatus() == OrderStatus.PAID
                        || order.getStatus() == OrderStatus.DELIVERED
                        || order.getStatus() == OrderStatus.SHIPPED
                        || order.getStatus() == OrderStatus.PROCESSING)
                .flatMap(order -> order.getItems().stream())
                .anyMatch(item -> item.getProduct().getId().equals(productId));

        if (!hasPurchased) {
            throw new BadRequestException(
                    "You can only review products you have purchased");
        }

        // Check customer hasn't already reviewed this product
        if (reviewRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            throw new BadRequestException(
                    "You have already reviewed this product");
        }

        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        return mapToResponse(reviewRepository.save(review));
    }

    public List<ReviewResponse> getProductReviews(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found");
        }
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ProductRatingResponse getProductRating(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Double averageRating = reviewRepository
                .findAverageRatingByProductId(productId);
        Long reviewCount = reviewRepository.countByProductId(productId);

        return ProductRatingResponse.builder()
                .productId(productId)
                .productName(product.getName())
                .averageRating(averageRating != null
                        ? Math.round(averageRating * 10.0) / 10.0 : 0.0)
                .reviewCount(reviewCount)
                .build();
    }

    @Transactional
    public void deleteReview(User user, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        boolean isAuthor = review.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isAuthor && !isAdmin) {
            throw new AccessDeniedException(
                    "You do not have permission to delete this review");
        }

        reviewRepository.delete(review);
    }

    // ---------- Mapper ----------

    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .userId(review.getUser().getId())
                .reviewerName(review.getUser().getFullName())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}