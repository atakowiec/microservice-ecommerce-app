package pl.atakowiec.ecommerce.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import pl.atakowiec.ecommerce.shared.response.ApiResponse;

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

    public ResponseEntity<ApiResponse<Void>> toResponseEntity() {
        return ResponseEntity
                .status(httpStatus)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error(httpStatus, getMessage()));
    }
}
