package com.peter.suuq.product;

import com.peter.suuq.exception.ResourceNotFoundException;
import com.peter.suuq.product.dto.ProductRequest;
import com.peter.suuq.product.dto.ProductResponse;
import com.peter.suuq.product.entity.Category;
import com.peter.suuq.product.entity.Product;
import com.peter.suuq.product.repository.CategoryRepository;
import com.peter.suuq.product.repository.ProductRepository;
import com.peter.suuq.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    private Category buildCategory() {
        return Category.builder()
                .id(1L)
                .name("Electronics")
                .build();
    }

    private Product buildProduct(Category category) {
        return Product.builder()
                .id(1L)
                .name("Wireless Earbuds")
                .description("Bluetooth earbuds")
                .price(new BigDecimal("15000.00"))
                .stockQuantity(50)
                .category(category)
                .active(true)
                .build();
    }

    @Test
    void createProduct_shouldReturnProductResponse_whenCategoryExists() {
        // Arrange
        Category category = buildCategory();

        ProductRequest request = new ProductRequest();
        request.setName("Wireless Earbuds");
        request.setDescription("Bluetooth earbuds");
        request.setPrice(new BigDecimal("15000.00"));
        request.setStockQuantity(50);
        request.setCategoryId(1L);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProductResponse response = productService.createProduct(request);

        // Assert
        assertThat(response.getName()).isEqualTo("Wireless Earbuds");
        assertThat(response.getPrice()).isEqualTo(new BigDecimal("15000.00"));
        assertThat(response.getCategoryName()).isEqualTo("Electronics");
        assertThat(response.isActive()).isTrue();

        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_shouldThrow_whenCategoryNotFound() {
        // Arrange
        ProductRequest request = new ProductRequest();
        request.setCategoryId(99L);

        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");

        verify(productRepository, never()).save(any());
    }

    @Test
    void getAllActiveProducts_shouldReturnOnlyActiveProducts() {
        // Arrange
        Category category = buildCategory();
        Product active = buildProduct(category);
        Product inactive = buildProduct(category);
        inactive.setActive(false);

        when(productRepository.findByActiveTrue()).thenReturn(List.of(active));

        // Act
        List<ProductResponse> responses = productService.getAllActiveProducts();

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("Wireless Earbuds");
    }

    @Test
    void deleteProduct_shouldSoftDelete_whenProductExists() {
        // Arrange
        Category category = buildCategory();
        Product product = buildProduct(category);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        productService.deleteProduct(1L);

        // Assert
        assertThat(product.isActive()).isFalse();
        verify(productRepository).save(product);
    }

    @Test
    void getProductById_shouldThrow_whenProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found");
    }
}