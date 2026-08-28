package pl.atakowiec.ecommerce.shared.exception;

import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pl.atakowiec.ecommerce.shared.response.ApiResponse;

@RestControllerAdvice
@Order(1)
public class HttpExceptionHandler {
    @ExceptionHandler(HttpException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpException(HttpException ex) {
        return ex.toResponseEntity();
    }
}
