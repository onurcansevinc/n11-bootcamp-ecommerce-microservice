package com.ecommerce.microservices.product_service.product.controller;

import com.ecommerce.microservices.product_service.common.exception.GlobalExceptionHandler;
import com.ecommerce.microservices.product_service.product.dto.ProductPatchRequest;
import com.ecommerce.microservices.product_service.product.dto.ProductResponse;
import com.ecommerce.microservices.product_service.product.dto.ProductUpsertRequest;
import com.ecommerce.microservices.product_service.product.exception.InvalidProductPatchException;
import com.ecommerce.microservices.product_service.product.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
		controllers = ProductController.class,
		excludeAutoConfiguration = {
				SecurityAutoConfiguration.class,
				OAuth2ResourceServerAutoConfiguration.class
		},
		properties = {
				"spring.cloud.config.enabled=false",
				"spring.cloud.config.import-check.enabled=false",
				"spring.cloud.discovery.enabled=false",
				"eureka.client.enabled=false"
		}
)
@Import(GlobalExceptionHandler.class)
class ProductControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private ProductService productService;

	@Test
	void getAllProductsReturnsPaginatedResponseMeta() throws Exception {
		ProductResponse productResponse = sampleProductResponse();
		when(productService.getAllProducts(0, 20, "keyboard", true, 3L, BigDecimal.TEN, BigDecimal.valueOf(999)))
				.thenReturn(new PageImpl<>(List.of(productResponse), PageRequest.of(0, 20), 1));

		mockMvc.perform(get("/api/v1/products")
						.param("page", "0")
						.param("size", "20")
						.param("search", "keyboard")
						.param("active", "true")
						.param("categoryId", "3")
						.param("minPrice", "10")
						.param("maxPrice", "999"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Products fetched successfully"))
				.andExpect(jsonPath("$.data[0].id").value(1))
				.andExpect(jsonPath("$.data[0].category.name").value("Accessories"))
				.andExpect(jsonPath("$.meta.page").value(0))
				.andExpect(jsonPath("$.meta.size").value(20))
				.andExpect(jsonPath("$.meta.totalElements").value(1))
				.andExpect(jsonPath("$.meta.totalPages").value(1));
	}

	@Test
	void createProductReturnsCreatedResponse() throws Exception {
		ProductUpsertRequest request = new ProductUpsertRequest(
				"Keyboard",
				"Mechanical keyboard",
				BigDecimal.valueOf(249.90),
				"SKU-1",
				true,
				"Öne Çıkan",
				3L
		);
		ProductResponse response = sampleProductResponse();
		when(productService.createProduct(request)).thenReturn(response);

		mockMvc.perform(post("/api/v1/products")
						.contentType(APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.message").value("Product created successfully"))
				.andExpect(jsonPath("$.data.id").value(1))
				.andExpect(jsonPath("$.data.name").value("Keyboard"))
				.andExpect(jsonPath("$.data.campaignLabel").value("Öne Çıkan"));
	}

	@Test
	void createProductReturnsBadRequestForInvalidBody() throws Exception {
		mockMvc.perform(post("/api/v1/products")
						.contentType(APPLICATION_JSON)
						.content("""
								{
								  "name": "",
								  "price": 0,
								  "sku": "",
								  "active": true,
								  "categoryId": 0
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Validation failed"))
				.andExpect(jsonPath("$.detail").value("Request body validation failed"));

		verify(productService, never()).createProduct(any());
	}

	@Test
	void patchProductMapsBusinessValidationToProblemDetail() throws Exception {
		ProductPatchRequest request = new ProductPatchRequest(null, null, null, null, null, null, null);
		when(productService.patchProduct(1L, request))
				.thenThrow(new InvalidProductPatchException("At least one field must be provided for patch"));

		mockMvc.perform(patch("/api/v1/products/1")
						.contentType(APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid product patch"))
				.andExpect(jsonPath("$.detail").value("At least one field must be provided for patch"));
	}

	@Test
	void deleteProductReturnsSuccessResponse() throws Exception {
		mockMvc.perform(delete("/api/v1/products/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Product deleted successfully"))
				.andExpect(jsonPath("$.data").doesNotExist());

		verify(productService).deleteProduct(1L);
	}

	private ProductResponse sampleProductResponse() {
		return new ProductResponse(
				1L,
				"Keyboard",
				"Mechanical keyboard",
				BigDecimal.valueOf(249.90),
				"SKU-1",
				true,
				"Öne Çıkan",
				"https://cdn.example.com/keyboard.jpg",
				new ProductResponse.CategorySummary(3L, "Accessories", "accessories"),
				LocalDateTime.now(),
				LocalDateTime.now()
		);
	}

}
