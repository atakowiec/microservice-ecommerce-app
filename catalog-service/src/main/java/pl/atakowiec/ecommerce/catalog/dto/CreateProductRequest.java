package pl.atakowiec.ecommerce.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import pl.atakowiec.ecommerce.catalog.model.ProductStatus;

public record CreateProductRequest(
        @NotBlank(message = "Product name is required")
        String productName,
        String description,
        @NotNull(message = "Price is required")
        @Positive(message = "Price must be greater than 0")
        Double price,
        @NotBlank(message = "Category is required")
        String categoryId,
        @NotNull(message = "Stock is required")
        @Min(value = 0, message = "Stock cannot be negative")
        Integer stock,
        @NotNull(message = "Status is required")
        ProductStatus status
) {
}
