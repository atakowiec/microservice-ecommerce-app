package pl.atakowiec.ecommerce.shared.response;

import org.springframework.http.HttpStatus;

public record ApiResponse<T>(int status, String message, T data) {

    public static <T> ApiResponse<T> of(HttpStatus status, String message, T data) {
        return new ApiResponse<>(status.value(), message, data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return of(HttpStatus.OK, message, data);
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return of(HttpStatus.CREATED, message, data);
    }

    public static ApiResponse<Void> error(HttpStatus status, String message) {
        return of(status, message, null);
    }
}
