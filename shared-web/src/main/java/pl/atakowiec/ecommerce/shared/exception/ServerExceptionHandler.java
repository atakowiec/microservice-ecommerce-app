package pl.atakowiec.ecommerce.shared.exception;

import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
@Order()
public class ServerExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleHttpException(Exception ex) {
        return new HttpException(500, ex.getMessage()).toResponseEntity();
    }
}
