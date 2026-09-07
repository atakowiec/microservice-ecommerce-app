package pl.atakowiec.ecommerce.catalog.service;

import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pl.atakowiec.ecommerce.catalog.dto.CreateProductRequest;
import pl.atakowiec.ecommerce.catalog.dto.ProductImageContent;
import pl.atakowiec.ecommerce.catalog.dto.UpdateProductRequest;
import pl.atakowiec.ecommerce.catalog.model.Category;
import pl.atakowiec.ecommerce.catalog.model.Product;
import pl.atakowiec.ecommerce.catalog.model.ProductImage;
import pl.atakowiec.ecommerce.catalog.repository.CategoryRepository;
import pl.atakowiec.ecommerce.catalog.repository.ProductRepository;
import pl.atakowiec.ecommerce.shared.exception.HttpException;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
@RequiredArgsConstructor
public class ProductsService {
    private static final SecureRandom EAN_RANDOM = new SecureRandom();

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final MongoTemplate mongoTemplate;
    private final ProductImageStorageService imageStorageService;

    public List<Product> findProducts(int page, int size, String sortBy, String direction, String query) {
        PageRequest pageRequest = PageRequest.of(page, size);
        ProductSortField sortField = ProductSortField.fromRequestValue(sortBy);
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        String normalizedQuery = query.trim();

        if (sortField.isCategory() || !normalizedQuery.isEmpty()) {
            return findProductsWithAggregation(pageRequest, sortField, sortDirection, normalizedQuery);
        }

        return productRepository
                .findNotDeleted(pageRequest.withSort(sortField.toSort(sortDirection)))
                .getContent();
    }

    public Product findProduct(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new HttpException(
                        HttpStatus.NOT_FOUND, "Product not found"));
    }

    public ProductImageContent findProductImage(String productId, int imageIndex) {
        Product product = findProduct(productId);
        List<ProductImage> images = product.getImages();
        if (images == null || imageIndex < 0 || imageIndex >= images.size()) {
            throw new HttpException(HttpStatus.NOT_FOUND, "Product image not found");
        }
        return imageStorageService.load(images.get(imageIndex));
    }

    public Product createProduct(
            CreateProductRequest request, List<MultipartFile> imageFiles) {
        String productName = request.productName().trim();
        if (productRepository.existsByProductNameIgnoreCase(productName)) {
            throw new HttpException(
                    HttpStatus.CONFLICT, "A product with this name already exists");
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new HttpException(
                        HttpStatus.BAD_REQUEST, "Selected category does not exist"));
        List<ProductImage> storedImages = imageStorageService.store(imageFiles);

        try {
            Product product = Product.builder()
                    .productName(productName)
                    .description(request.description() == null ? "" : request.description().trim())
                    .price(request.price())
                    .ean(generateEan())
                    .stock(request.stock())
                    .status(request.status())
                    .images(storedImages)
                    .category(category)
                    .build();
            return productRepository.save(product);
        } catch (RuntimeException exception) {
            imageStorageService.delete(storedImages);
            throw exception;
        }
    }

    public Product updateProduct(
            String productId,
            UpdateProductRequest request,
            List<MultipartFile> imageFiles) {
        Product product = findProduct(productId);
        String productName = request.productName().trim();
        if (productRepository.existsByProductNameIgnoreCaseAndIdNot(productName, productId)) {
            throw new HttpException(
                    HttpStatus.CONFLICT, "A product with this name already exists");
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new HttpException(
                        HttpStatus.BAD_REQUEST, "Selected category does not exist"));
        boolean replacingImages = imageFiles != null && !imageFiles.isEmpty();
        List<ProductImage> previousImages = product.getImages() == null
                ? List.of()
                : product.getImages();
        List<ProductImage> replacementImages = replacingImages
                ? imageStorageService.store(imageFiles)
                : previousImages;

        product.setProductName(productName);
        product.setDescription(request.description() == null ? "" : request.description().trim());
        product.setPrice(request.price());
        product.setCategory(category);
        product.setStock(request.stock().intValueExact());
        product.setStatus(request.status());
        product.setImages(replacementImages);

        try {
            Product updatedProduct = productRepository.save(product);
            if (replacingImages) {
                imageStorageService.deleteIgnoringErrors(previousImages);
            }
            return updatedProduct;
        } catch (RuntimeException exception) {
            if (replacingImages) {
                imageStorageService.deleteIgnoringErrors(replacementImages);
            }
            throw exception;
        }
    }

    private List<Product> findProductsWithAggregation(
            PageRequest pageRequest,
            ProductSortField sortField,
            Sort.Direction direction,
            String query) {
        List<AggregationOperation> operations = new ArrayList<>();
        operations.add(lookup("categories", "category", "_id", "resolvedCategory"));
        operations.add(unwind("resolvedCategory"));
        operations.add(match(Criteria.where("deleted").is(false)));

        if (!query.isEmpty()) {
            Pattern searchPattern = Pattern.compile(
                    Pattern.quote(query), Pattern.CASE_INSENSITIVE);
            operations.add(match(new Criteria().orOperator(
                    Criteria.where("productName").regex(searchPattern),
                    Criteria.where("ean").regex(searchPattern),
                    Criteria.where("resolvedCategory.name").regex(searchPattern))));
        }

        String sortProperty = sortField.isCategory()
                ? "resolvedCategory.name"
                : sortField.mongoProperty();
        Sort productSort = Sort.by(direction, sortProperty)
                .and(Sort.by(Sort.Direction.ASC, "_id"));

        operations.add(sort(productSort));
        operations.add(skip(pageRequest.getOffset()));
        operations.add(limit(pageRequest.getPageSize()));
        operations.add(project("_id"));

        Aggregation aggregation = newAggregation(operations);

        List<String> productIds = mongoTemplate
                .aggregate(aggregation, "products", Document.class)
                .getMappedResults()
                .stream()
                .map(document -> document.get("_id").toString())
                .toList();
        Map<String, Product> productsById = productRepository.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        return productIds.stream()
                .map(productsById::get)
                .toList();
    }

    private String generateEan() {
        String ean;
        do {
            StringBuilder firstTwelveDigits = new StringBuilder(12);
            for (int index = 0; index < 12; index++) {
                firstTwelveDigits.append(EAN_RANDOM.nextInt(10));
            }
            ean = firstTwelveDigits + String.valueOf(checkDigit(firstTwelveDigits));
        } while (productRepository.existsByEan(ean));
        return ean;
    }

    private int checkDigit(CharSequence firstTwelveDigits) {
        int sum = 0;
        for (int index = 0; index < firstTwelveDigits.length(); index++) {
            int digit = Character.digit(firstTwelveDigits.charAt(index), 10);
            sum += index % 2 == 0 ? digit : digit * 3;
        }
        return (10 - sum % 10) % 10;
    }

    public Product deleteProduct(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new HttpException(404, "Product with id " + productId + " does not exist."));

        product.setDeleted(true);

        productRepository.save(product);

        return product;
    }
}
