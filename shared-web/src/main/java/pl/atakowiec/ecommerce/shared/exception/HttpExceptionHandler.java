package pl.atakowiec.ecommerce.shared.exception;

import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import pl.atakowiec.ecommerce.shared.response.ApiResponse;

@RestControllerAdvice
@Order(1)
public class HttpExceptionHandler {
    @ExceptionHandler(HttpException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpException(HttpException ex) {
        return ex.toResponseEntity();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Request validation failed");
        return new HttpException(400, message).toResponseEntity();
    }
}
