package pl.atakowiec.ecommerce.order;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import pl.atakowiec.ecommerce.shared.response.ApiResponse;

import java.util.Map;

@RestController
public class TestController {
    @GetMapping("/test")
    public ApiResponse<Map<String, Object>> test(@RequestHeader HttpHeaders headers) {
        return ApiResponse.ok("Order service is available", Map.of(
                "service", "order-service",
                "headers", headers
        ));
    }
}
