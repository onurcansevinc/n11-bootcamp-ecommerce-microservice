package com.ecommerce.microservices.product_service.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductPatchRequest(
        @Pattern(regexp = ".*\\S.*", message = "must not be blank")
        @Size(max = 150)
        String name,

        @Size(max = 1000)
        String description,

        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal price,

        @Pattern(regexp = ".*\\S.*", message = "must not be blank")
        @Size(max = 100)
        String sku,

        Boolean active,

        @Positive
        Long categoryId
) {
    public boolean hasChanges() {
        return name != null
                || description != null
                || price != null
                || sku != null
                || active != null
                || categoryId != null;
    }
}
