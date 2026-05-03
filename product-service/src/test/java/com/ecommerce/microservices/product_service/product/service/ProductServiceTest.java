package com.ecommerce.microservices.product_service.product.service;

import com.ecommerce.microservices.product_service.category.repository.CategoryRepository;
import com.ecommerce.microservices.product_service.product.dto.ProductResponse;
import com.ecommerce.microservices.product_service.product.entity.Product;
import com.ecommerce.microservices.product_service.product.entity.ProductImage;
import com.ecommerce.microservices.product_service.product.repository.ProductImageRepository;
import com.ecommerce.microservices.product_service.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(categoryRepository, productRepository, productImageRepository);
    }

    @Test
    void getProductById_shouldIncludeMainImageUrl() {
        Product product = createProduct(1L, "Sony Kulaklik", "SKU-1");
        ProductImage productImage = createProductImage(11L, "https://cdn.example.com/sony-main.jpg", true, product);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productImageRepository.findFirstByProductIdOrderByMainImageDescIdAsc(1L))
                .thenReturn(Optional.of(productImage));

        ProductResponse response = productService.getProductById(1L);

        assertThat(response.mainImageUrl()).isEqualTo("https://cdn.example.com/sony-main.jpg");
    }

    @Test
    void getAllProducts_shouldMapMainImageUrlPerProduct() {
        Product firstProduct = createProduct(1L, "Sony Kulaklik", "SKU-1");
        Product secondProduct = createProduct(2L, "Philips Kahve Makinesi", "SKU-2");

        ProductImage secondImage = createProductImage(21L, "https://cdn.example.com/philips-main.jpg", true, secondProduct);
        ProductImage firstImage = createProductImage(22L, "https://cdn.example.com/sony-main.jpg", true, firstProduct);

        Page<Product> productPage = new PageImpl<>(List.of(firstProduct, secondProduct));

        when(productRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Product>>any(), any(Pageable.class)))
                .thenReturn(productPage);
        when(productImageRepository.findByProductIdInOrderByProductIdAscMainImageDescIdAsc(List.of(1L, 2L)))
                .thenReturn(List.of(firstImage, secondImage));

        Page<ProductResponse> responsePage = productService.getAllProducts(0, 20, null, true, null, null, null);

        assertThat(responsePage.getContent())
                .extracting(ProductResponse::mainImageUrl)
                .containsExactly("https://cdn.example.com/sony-main.jpg", "https://cdn.example.com/philips-main.jpg");
    }

    private Product createProduct(Long id, String name, String sku) {
        Product product = new Product(
                name,
                "Aciklama",
                BigDecimal.valueOf(1999.90),
                sku,
                true,
                "Öne Çıkan",
                null
        );

        ReflectionTestUtils.setField(product, "id", id);
        ReflectionTestUtils.setField(product, "createdAt", LocalDateTime.now().minusDays(1));
        ReflectionTestUtils.setField(product, "updatedAt", LocalDateTime.now());
        return product;
    }

    private ProductImage createProductImage(Long id, String imageUrl, boolean mainImage, Product product) {
        ProductImage productImage = new ProductImage(imageUrl, product);
        productImage.setMainImage(mainImage);
        ReflectionTestUtils.setField(productImage, "id", id);
        return productImage;
    }
}
