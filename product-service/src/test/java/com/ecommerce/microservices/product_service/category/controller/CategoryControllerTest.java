package com.ecommerce.microservices.product_service.category.controller;

import com.ecommerce.microservices.product_service.category.dto.CategoryResponse;
import com.ecommerce.microservices.product_service.category.dto.CategoryUpsertRequest;
import com.ecommerce.microservices.product_service.category.exception.CategoryInUseException;
import com.ecommerce.microservices.product_service.category.service.CategoryService;
import com.ecommerce.microservices.product_service.common.exception.GlobalExceptionHandler;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
		controllers = CategoryController.class,
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
class CategoryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private CategoryService categoryService;

	@Test
	void getAllCategoriesReturnsPaginatedResponseMeta() throws Exception {
		when(categoryService.getAllCategories(0, 10, "tech", true))
				.thenReturn(new PageImpl<>(
						List.of(new CategoryResponse(3L, "Tech", "tech", true)),
						PageRequest.of(0, 10),
						1
				));

		mockMvc.perform(get("/api/v1/categories")
						.param("page", "0")
						.param("size", "10")
						.param("search", "tech")
						.param("active", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Categories fetched successfully"))
				.andExpect(jsonPath("$.data[0].slug").value("tech"))
				.andExpect(jsonPath("$.meta.page").value(0))
				.andExpect(jsonPath("$.meta.size").value(10))
				.andExpect(jsonPath("$.meta.totalElements").value(1));
	}

	@Test
	void createCategoryReturnsCreatedResponse() throws Exception {
		CategoryUpsertRequest request = new CategoryUpsertRequest("Tech", "tech", true);
		when(categoryService.createCategory(request)).thenReturn(new CategoryResponse(3L, "Tech", "tech", true));

		mockMvc.perform(post("/api/v1/categories")
						.contentType(APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.message").value("Category created successfully"))
				.andExpect(jsonPath("$.data.id").value(3))
				.andExpect(jsonPath("$.data.name").value("Tech"));
	}

	@Test
	void createCategoryReturnsBadRequestForInvalidBody() throws Exception {
		mockMvc.perform(post("/api/v1/categories")
						.contentType(APPLICATION_JSON)
						.content("""
								{
								  "name": "",
								  "slug": "",
								  "active": null
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Validation failed"))
				.andExpect(jsonPath("$.detail").value("Request body validation failed"));

		verify(categoryService, never()).createCategory(any());
	}

	@Test
	void deleteCategoryMapsBusinessConflictToProblemDetail() throws Exception {
		doThrow(new CategoryInUseException(3L)).when(categoryService).deleteCategory(3L);

		mockMvc.perform(delete("/api/v1/categories/3"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("Category in use"))
				.andExpect(jsonPath("$.detail").value("Category with id 3 is in use by one or more products"));
	}

	@Test
	void getCategoryByIdReturnsCategoryResponse() throws Exception {
		when(categoryService.getCategoryById(3L)).thenReturn(new CategoryResponse(3L, "Tech", "tech", true));

		mockMvc.perform(get("/api/v1/categories/3"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Category fetched successfully"))
				.andExpect(jsonPath("$.data.id").value(3))
				.andExpect(jsonPath("$.data.active").value(true));
	}

}
