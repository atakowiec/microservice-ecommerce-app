package pl.atakowiec.ecommerce.catalog.service;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import pl.atakowiec.ecommerce.shared.exception.HttpException;

import java.util.Arrays;

enum ProductSortField {
    NAME("name", "productName"),
    EAN("ean", "ean"),
    STOCK("stock", "stock"),
    CATEGORY("category", "category"),
    PRICE("price", "price");

    private final String requestValue;
    private final String mongoProperty;

    ProductSortField(String requestValue, String mongoProperty) {
        this.requestValue = requestValue;
        this.mongoProperty = mongoProperty;
    }

    static ProductSortField fromRequestValue(String value) {
        return Arrays.stream(values())
                .filter(field -> field.requestValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new HttpException(HttpStatus.BAD_REQUEST,
                        "Unsupported product sort field: " + value));
    }

    Sort toSort(Sort.Direction direction) {
        Sort requestedSort = Sort.by(direction, mongoProperty);

        return requestedSort.and(Sort.by(Sort.Direction.ASC, "_id"));
    }

    boolean isCategory() {
        return this == CATEGORY;
    }

    String mongoProperty() {
        return mongoProperty;
    }
}
