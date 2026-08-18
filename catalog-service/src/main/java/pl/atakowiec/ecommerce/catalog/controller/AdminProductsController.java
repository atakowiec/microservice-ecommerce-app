package pl.atakowiec.ecommerce.catalog.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.atakowiec.ecommerce.catalog.model.Product;
import pl.atakowiec.ecommerce.catalog.service.ProductsService;
import pl.atakowiec.ecommerce.shared.response.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductsController {
    private final ProductsService productsService;

    @GetMapping
    public ApiResponse<List<Product>> findProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "") String query) {
        return ApiResponse.ok(
                "Products retrieved successfully",
                productsService.findProducts(page, size, sortBy, direction, query));
    }
}
