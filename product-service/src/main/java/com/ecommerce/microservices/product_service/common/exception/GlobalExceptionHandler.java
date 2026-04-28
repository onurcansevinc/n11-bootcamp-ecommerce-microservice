package com.ecommerce.microservices.product_service.common.exception;

import com.ecommerce.microservices.product_service.category.exception.CategoryInUseException;
import com.ecommerce.microservices.product_service.category.exception.CategoryNotFoundException;
import com.ecommerce.microservices.product_service.category.exception.DuplicateCategorySlugException;
import com.ecommerce.microservices.product_service.category.exception.InvalidCategoryPatchException;
import com.ecommerce.microservices.product_service.product.exception.DuplicateProductSkuException;
import com.ecommerce.microservices.product_service.product.exception.InvalidProductFilterException;
import com.ecommerce.microservices.product_service.product.exception.InvalidProductPatchException;
import com.ecommerce.microservices.product_service.product.exception.ProductNotFoundException;
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

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFound(ProductNotFoundException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problemDetail.setTitle("Product not found");
        return problemDetail;
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ProblemDetail handleCategoryNotFound(CategoryNotFoundException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problemDetail.setTitle("Category not found");
        return problemDetail;
    }

    @ExceptionHandler(CategoryInUseException.class)
    public ProblemDetail handleCategoryInUse(CategoryInUseException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problemDetail.setTitle("Category in use");
        return problemDetail;
    }

    @ExceptionHandler(DuplicateProductSkuException.class)
    public ProblemDetail handleDuplicateProductSku(DuplicateProductSkuException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problemDetail.setTitle("Duplicate product sku");
        return problemDetail;
    }

    @ExceptionHandler(DuplicateCategorySlugException.class)
    public ProblemDetail handleDuplicateCategorySlug(DuplicateCategorySlugException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problemDetail.setTitle("Duplicate category slug");
        return problemDetail;
    }

    @ExceptionHandler(InvalidProductFilterException.class)
    public ProblemDetail handleInvalidProductFilter(InvalidProductFilterException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problemDetail.setTitle("Invalid product filter");
        return problemDetail;
    }

    @ExceptionHandler(InvalidProductPatchException.class)
    public ProblemDetail handleInvalidProductPatch(InvalidProductPatchException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problemDetail.setTitle("Invalid product patch");
        return problemDetail;
    }

    @ExceptionHandler(InvalidCategoryPatchException.class)
    public ProblemDetail handleInvalidCategoryPatch(InvalidCategoryPatchException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problemDetail.setTitle("Invalid category patch");
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
