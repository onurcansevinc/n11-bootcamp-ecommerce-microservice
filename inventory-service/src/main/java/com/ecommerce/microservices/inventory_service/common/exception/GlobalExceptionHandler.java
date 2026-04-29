package com.ecommerce.microservices.inventory_service.common.exception;

import com.ecommerce.microservices.inventory_service.inventory.exception.InsufficientInventoryException;
import com.ecommerce.microservices.inventory_service.inventory.exception.InvalidInventoryReservationException;
import com.ecommerce.microservices.inventory_service.inventory.exception.InventoryItemNotFoundException;
import com.ecommerce.microservices.inventory_service.inventory.exception.InventoryReservationNotFoundException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
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

	@ExceptionHandler(InventoryItemNotFoundException.class)
	public ProblemDetail handleInventoryItemNotFound(InventoryItemNotFoundException exception) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
		problemDetail.setTitle("Inventory not found");
		return problemDetail;
	}

	@ExceptionHandler(InventoryReservationNotFoundException.class)
	public ProblemDetail handleInventoryReservationNotFound(InventoryReservationNotFoundException exception) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
		problemDetail.setTitle("Inventory reservation not found");
		return problemDetail;
	}

	@ExceptionHandler(InsufficientInventoryException.class)
	public ProblemDetail handleInsufficientInventory(InsufficientInventoryException exception) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
		problemDetail.setTitle("Insufficient inventory");
		return problemDetail;
	}

	@ExceptionHandler(InvalidInventoryReservationException.class)
	public ProblemDetail handleInvalidInventoryReservation(InvalidInventoryReservationException exception) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
		problemDetail.setTitle("Invalid inventory reservation");
		return problemDetail;
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ProblemDetail handleOptimisticLockingFailure(OptimisticLockingFailureException exception) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
				HttpStatus.CONFLICT,
				"Inventory was modified concurrently, please retry the request"
		);
		problemDetail.setTitle("Concurrent inventory modification");
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
