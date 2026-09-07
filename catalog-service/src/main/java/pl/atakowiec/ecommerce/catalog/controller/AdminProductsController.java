package pl.atakowiec.ecommerce.catalog.controller;

import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.atakowiec.ecommerce.catalog.dto.CreateProductRequest;
import pl.atakowiec.ecommerce.catalog.dto.UpdateProductRequest;
import pl.atakowiec.ecommerce.catalog.dto.ProductImageContent;
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

    @GetMapping("/{productId}")
    public ApiResponse<Product> findProduct(@PathVariable String productId) {
        return ApiResponse.ok(
                "Product retrieved successfully",
                productsService.findProduct(productId));
    }

    @GetMapping("/{productId}/images/{imageIndex}")
    public ResponseEntity<byte[]> findProductImage(
            @PathVariable String productId,
            @PathVariable int imageIndex) {
        ProductImageContent image = productsService.findProductImage(productId, imageIndex);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(image.fileName())
                                .build()
                                .toString())
                .body(image.bytes());
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Product> createProduct(
            @Valid @RequestPart("product") CreateProductRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return ApiResponse.created(
                "Product created successfully",
                productsService.createProduct(request, images));
    }

    @PutMapping(path = "/{productId}", consumes = "multipart/form-data")
    public ApiResponse<Product> updateProduct(
            @PathVariable String productId,
            @Valid @RequestPart("product") UpdateProductRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return ApiResponse.ok(
                "Product updated successfully",
                productsService.updateProduct(productId, request, images));
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Product> deleteProduct(@PathVariable String productId) {
        return ApiResponse.ok(
                "Product deleted successfully",
                productsService.deleteProduct(productId)
        );
    }
}
