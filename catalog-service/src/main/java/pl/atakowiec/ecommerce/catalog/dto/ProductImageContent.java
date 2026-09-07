package pl.atakowiec.ecommerce.catalog.dto;

public record ProductImageContent(
        byte[] bytes,
        String contentType,
        String fileName
) {
}
