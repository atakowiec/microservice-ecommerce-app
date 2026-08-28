package pl.atakowiec.ecommerce.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@Getter
public class HttpException extends RuntimeException {
    private final HttpStatus httpStatus;

    public HttpException(HttpStatus httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpException(int statusCode, String message) {
        this(HttpStatus.valueOf(statusCode), message);
    }

    public ResponseEntity<Map<String, String>> toResponseEntity() {
        return ResponseEntity
                .status(httpStatus)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("error", httpStatus.getReasonPhrase(),
                        "message", getMessage(),
                        "status", String.valueOf(httpStatus.value())));
    }
}