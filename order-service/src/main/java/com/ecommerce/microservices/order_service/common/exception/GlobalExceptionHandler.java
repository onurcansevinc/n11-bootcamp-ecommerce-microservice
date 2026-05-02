package com.ecommerce.microservices.order_service.common.exception;

import com.ecommerce.microservices.order_service.order.exception.CartAccessDeniedForOrderException;
import com.ecommerce.microservices.order_service.order.exception.CartNotFoundForOrderException;
import com.ecommerce.microservices.order_service.order.exception.CartServiceUnavailableException;
import com.ecommerce.microservices.order_service.order.exception.EmptyCartForOrderException;
import com.ecommerce.microservices.order_service.order.exception.InventoryReservationFailedException;
import com.ecommerce.microservices.order_service.order.exception.InventoryServiceUnavailableException;
import com.ecommerce.microservices.order_service.order.exception.InvalidOrderStateException;
import com.ecommerce.microservices.order_service.order.exception.OrderAccessDeniedException;
import com.ecommerce.microservices.order_service.order.exception.OrderNotFoundException;
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

	@ExceptionHandler(OrderNotFoundException.class)
	public ProblemDetail handleOrderNotFound(OrderNotFoundException exception) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
		problemDetail.setTitle("Order not found");
		return problemDetail;
	}

	@ExceptionHandler(CartNotFoundForOrderException.class)
	public ProblemDetail handleCartNotFoundForOrder(CartNotFoundForOrderException exception) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
		problemDetail.setTitle("Cart not found");
		return problemDetail;
	}

	@ExceptionHandler({OrderAccessDeniedException.class, CartAccessDeniedForOrderException.class})
	public ProblemDetail handleOrderAccessDenied(RuntimeException exception) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
		problemDetail.setTitle("Access denied");
		return problemDetail;
	}

	@ExceptionHandler(EmptyCartForOrderException.class)
	public ProblemDetail handleEmptyCartForOrder(EmptyCartForOrderException exception) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
		problemDetail.setTitle("Empty cart");
		return problemDetail;
	}

	@ExceptionHandler(InvalidOrderStateException.class)
	public ProblemDetail handleInvalidOrderState(InvalidOrderStateException exception) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
		problemDetail.setTitle("Invalid order state");
		return problemDetail;
	}

	@ExceptionHandler(InventoryReservationFailedException.class)
	public ProblemDetail handleInventoryReservationFailed(InventoryReservationFailedException exception) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
		problemDetail.setTitle("Inventory reservation failed");
		return problemDetail;
	}

	@ExceptionHandler({CartServiceUnavailableException.class, InventoryServiceUnavailableException.class})
	public ProblemDetail handleRemoteServiceUnavailable(RuntimeException exception) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
		problemDetail.setTitle("Dependent service unavailable");
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
