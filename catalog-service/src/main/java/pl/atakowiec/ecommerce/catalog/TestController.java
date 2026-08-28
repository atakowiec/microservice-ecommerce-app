package pl.atakowiec.ecommerce.catalog;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestController {
    @GetMapping("/test")
    public Map<String, Object> test(@RequestHeader HttpHeaders headers) {
        return Map.of(
                "service", "catalog-service",
                "status", "ok",
                "headers", headers
        );
    }
}
