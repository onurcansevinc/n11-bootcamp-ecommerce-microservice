package com.ecommerce.microservices.product_service.category.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryPatchRequest(
        @Pattern(regexp = ".*\\S.*", message = "must not be blank")
        @Size(max = 100)
        String name,

        @Pattern(regexp = ".*\\S.*", message = "must not be blank")
        @Size(max = 120)
        String slug,

        Boolean active
) {
    public boolean hasChanges() {
        return name != null || slug != null || active != null;
    }
}
