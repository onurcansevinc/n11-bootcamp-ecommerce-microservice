package com.ecommerce.microservices.payment_service.common.exception;

import com.ecommerce.microservices.payment_service.payment.exception.DuplicatePendingPaymentException;
import com.ecommerce.microservices.payment_service.payment.exception.InvalidPaymentStateException;
import com.ecommerce.microservices.payment_service.payment.exception.OrderAccessDeniedForPaymentException;
import com.ecommerce.microservices.payment_service.payment.exception.OrderNotFoundForPaymentException;
import com.ecommerce.microservices.payment_service.payment.exception.OrderNotPayableException;
import com.ecommerce.microservices.payment_service.payment.exception.OrderServiceUnavailableForPaymentException;
import com.ecommerce.microservices.payment_service.payment.exception.PaymentAccessDeniedException;
import com.ecommerce.microservices.payment_service.payment.exception.PaymentGatewayInitializationException;
import com.ecommerce.microservices.payment_service.payment.exception.PaymentGatewayVerificationException;
import com.ecommerce.microservices.payment_service.payment.exception.PaymentNotFoundException;
import com.ecommerce.microservices.payment_service.payment.exception.UnsupportedPaymentProviderException;
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

	@ExceptionHandler({PaymentNotFoundException.class, OrderNotFoundForPaymentException.class})
	public ProblemDetail handleNotFound(RuntimeException exception) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
		problemDetail.setTitle("Resource not found");
		return problemDetail;
	}

	@ExceptionHandler({PaymentAccessDeniedException.class, OrderAccessDeniedForPaymentException.class})
	public ProblemDetail handleAccessDenied(RuntimeException exception) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
		problemDetail.setTitle("Access denied");
		return problemDetail;
	}

	@ExceptionHandler({
			DuplicatePendingPaymentException.class,
			InvalidPaymentStateException.class,
			OrderNotPayableException.class,
			UnsupportedPaymentProviderException.class
	})
	public ProblemDetail handleConflict(RuntimeException exception) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
		problemDetail.setTitle("Payment request conflict");
		return problemDetail;
	}

	@ExceptionHandler(OrderServiceUnavailableForPaymentException.class)
	public ProblemDetail handleRemoteServiceUnavailable(OrderServiceUnavailableForPaymentException exception) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
		problemDetail.setTitle("Dependent service unavailable");
		return problemDetail;
	}

	@ExceptionHandler({PaymentGatewayInitializationException.class, PaymentGatewayVerificationException.class})
	public ProblemDetail handleGatewayFailure(RuntimeException exception) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, exception.getMessage());
		problemDetail.setTitle("Payment gateway failure");
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
