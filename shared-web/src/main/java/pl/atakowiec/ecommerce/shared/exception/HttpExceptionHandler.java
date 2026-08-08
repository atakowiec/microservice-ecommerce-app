package pl.atakowiec.ecommerce.shared.exception;

import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
@Order(1)
public class HttpExceptionHandler {
    @ExceptionHandler(HttpException.class)
    public ResponseEntity<Map<String, String>> handleHttpException(HttpException ex) {
        return ex.toResponseEntity();
    }
}