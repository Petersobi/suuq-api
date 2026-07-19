package com.peter.suuq.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRatingResponse {
    private Long productId;
    private String productName;
    private Double averageRating;
    private Long reviewCount;
}