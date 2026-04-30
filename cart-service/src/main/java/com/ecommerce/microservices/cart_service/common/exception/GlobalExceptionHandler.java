package com.ecommerce.microservices.cart_service.common.exception;

import com.ecommerce.microservices.cart_service.cart.exception.CartAccessDeniedException;
import com.ecommerce.microservices.cart_service.cart.exception.CartItemNotFoundException;
import com.ecommerce.microservices.cart_service.cart.exception.CartNotFoundException;
import com.ecommerce.microservices.cart_service.cart.exception.InactiveProductException;
import com.ecommerce.microservices.cart_service.cart.exception.InvalidCartStateException;
import com.ecommerce.microservices.cart_service.cart.exception.ProductCatalogUnavailableException;
import com.ecommerce.microservices.cart_service.cart.exception.ProductNotFoundForCartException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(CartNotFoundException.class)
	public ProblemDetail handleCartNotFound(CartNotFoundException exception) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
		problemDetail.setTitle("Cart not found");
		return problemDetail;
	}

	@ExceptionHandler(CartItemNotFoundException.class)
	public ProblemDetail handleCartItemNotFound(CartItemNotFoundException exception) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
		problemDetail.setTitle("Cart item not found");
		return problemDetail;
	}

	@ExceptionHandler(ProductNotFoundForCartException.class)
	public ProblemDetail handleProductNotFoundForCart(ProductNotFoundForCartException exception) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
		problemDetail.setTitle("Product not found");
		return problemDetail;
	}

	@ExceptionHandler(CartAccessDeniedException.class)
	public ProblemDetail handleCartAccessDenied(CartAccessDeniedException exception) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
		problemDetail.setTitle("Cart access denied");
		return problemDetail;
	}

	@ExceptionHandler(InvalidCartStateException.class)
	public ProblemDetail handleInvalidCartState(InvalidCartStateException exception) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
		problemDetail.setTitle("Invalid cart state");
		return problemDetail;
	}

	@ExceptionHandler(InactiveProductException.class)
	public ProblemDetail handleInactiveProduct(InactiveProductException exception) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
		problemDetail.setTitle("Inactive product");
		return problemDetail;
	}

	@ExceptionHandler(ProductCatalogUnavailableException.class)
	public ProblemDetail handleProductCatalogUnavailable(ProductCatalogUnavailableException exception) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
		problemDetail.setTitle("Product catalog unavailable");
		return problemDetail;
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
		String detail = exception.getConstraintViolations()
				.stream()
				.map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
				.sorted()
				.collect(Collectors.joining(", "));

		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
		problemDetail.setTitle("Validation failed");
		return problemDetail;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
		List<Map<String, String>> errors = exception.getBindingResult()
				.getFieldErrors()
				.stream()
				.sorted(Comparator.comparing(FieldError::getField))
				.map(fieldError -> Map.of(
						"field", fieldError.getField(),
						"message", Objects.requireNonNullElse(fieldError.getDefaultMessage(), "Invalid value")
				))
				.toList();

		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_REQUEST,
				"Request body validation failed"
		);
		problemDetail.setTitle("Validation failed");
		problemDetail.setProperty("errors", errors);
		return problemDetail;
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
		Throwable rootCause = exception.getMostSpecificCause();

		if (rootCause instanceof UnrecognizedPropertyException unrecognizedPropertyException) {
			ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
					HttpStatus.BAD_REQUEST,
					"Unknown field '" + unrecognizedPropertyException.getPropertyName() + "' in request body"
			);
			problemDetail.setTitle("Malformed request body");
			return problemDetail;
		}

		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_REQUEST,
				"Request body is malformed or unreadable"
		);
		problemDetail.setTitle("Malformed request body");
		return problemDetail;
	}
}
