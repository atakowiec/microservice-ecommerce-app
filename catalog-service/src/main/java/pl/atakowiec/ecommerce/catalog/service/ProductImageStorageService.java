package pl.atakowiec.ecommerce.catalog.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pl.atakowiec.ecommerce.catalog.model.ProductImage;
import pl.atakowiec.ecommerce.catalog.dto.ProductImageContent;
import pl.atakowiec.ecommerce.shared.exception.HttpException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
class ProductImageStorageService {
    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private final S3Client s3Client;
    private final String bucket;

    ProductImageStorageService(S3Client s3Client, @Value("${aws.s3.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    List<ProductImage> store(List<MultipartFile> imageFiles) {
        if (imageFiles == null || imageFiles.isEmpty()) {
            return List.of();
        }

        List<ProductImage> storedImages = new ArrayList<>();
        try {
            for (MultipartFile imageFile : imageFiles) {
                validate(imageFile);
                storedImages.add(store(imageFile));
            }
            return List.copyOf(storedImages);
        } catch (IOException | SdkException exception) {
            deleteIgnoringErrors(storedImages);
            throw new HttpException(
                    HttpStatus.BAD_GATEWAY, "Product images could not be uploaded");
        } catch (RuntimeException exception) {
            deleteIgnoringErrors(storedImages);
            throw exception;
        }
    }

    void delete(List<ProductImage> images) {
        if (images == null || images.isEmpty()) {
            return;
        }

        for (ProductImage image : images) {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(image.id())
                    .build();
            s3Client.deleteObject(request);
        }
    }

    ProductImageContent load(ProductImage image) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(image.id())
                .build();
        try {
            var response = s3Client.getObjectAsBytes(request);
            String responseContentType = response.response().contentType();
            return new ProductImageContent(
                    response.asByteArray(),
                    responseContentType == null ? image.contentType() : responseContentType,
                    image.fileName());
        } catch (S3Exception exception) {
            if (exception.statusCode() == HttpStatus.NOT_FOUND.value()) {
                throw new HttpException(HttpStatus.NOT_FOUND, "Product image not found");
            }
            throw new HttpException(HttpStatus.BAD_GATEWAY, "Product image could not be loaded");
        } catch (SdkException exception) {
            throw new HttpException(HttpStatus.BAD_GATEWAY, "Product image could not be loaded");
        }
    }

    private ProductImage store(MultipartFile imageFile) throws IOException {
        String contentType = imageFile.getContentType();
        String key = "products/" + UUID.randomUUID()
                + "." + ALLOWED_IMAGE_TYPES.get(contentType);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .contentLength(imageFile.getSize())
                .build();

        try (InputStream inputStream = imageFile.getInputStream()) {
            s3Client.putObject(
                    request,
                    RequestBody.fromInputStream(inputStream, imageFile.getSize()));
        }

        return new ProductImage(
                key,
                imageFile.getOriginalFilename(),
                contentType);
    }

    private void validate(MultipartFile imageFile) {
        String contentType = imageFile.getContentType();
        if (imageFile.isEmpty()) {
            throw new HttpException(
                    HttpStatus.BAD_REQUEST, "Empty images cannot be uploaded");
        }
        if (!ALLOWED_IMAGE_TYPES.containsKey(contentType)) {
            throw new HttpException(
                    HttpStatus.BAD_REQUEST, "Only JPEG, PNG, and WebP images are supported");
        }
        if (imageFile.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new HttpException(
                    HttpStatus.BAD_REQUEST, "An image cannot be larger than 5 MB");
        }
    }

    void deleteIgnoringErrors(List<ProductImage> images) {
        try {
            delete(images);
        } catch (SdkException ignored) {
            // Best-effort rollback: do not hide the original upload error.
        }
    }
}
