package pl.atakowiec.ecommerce.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import pl.atakowiec.ecommerce.catalog.model.ProductStatus;

import java.math.BigDecimal;

public record UpdateProductRequest(
        @NotBlank(message = "Product name is required")
        String productName,
        String description,
        @NotNull(message = "Price is required")
        @Positive(message = "Price must be greater than 0")
        Double price,
        @NotBlank(message = "Category is required")
        String categoryId,
        @NotNull(message = "Stock is required")
        @DecimalMin(value = "0", message = "Stock cannot be negative")
        @Digits(integer = 9, fraction = 0, message = "Stock must be a whole number")
        BigDecimal stock,
        @NotNull(message = "Status is required")
        ProductStatus status
) {
}
