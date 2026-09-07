package pl.atakowiec.ecommerce.catalog;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import pl.atakowiec.ecommerce.catalog.model.Category;
import pl.atakowiec.ecommerce.catalog.model.Product;
import pl.atakowiec.ecommerce.catalog.model.ProductImage;
import pl.atakowiec.ecommerce.catalog.repository.CategoryRepository;
import pl.atakowiec.ecommerce.catalog.repository.ProductRepository;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "aws.s3.bucket=test-product-images",
        "aws.s3.region=eu-central-1"
})
public class ProductsServiceIntegrationTest {
    @Container
    @ServiceConnection
    static final MongoDBContainer mongodb = new MongoDBContainer("mongo:8.3.8");

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private S3Client s3Client;

    @BeforeEach
    void setUpUsers() {
        categoryRepository.deleteAll();
        productRepository.deleteAll();

        List<Category> categories = List.of(
                category("Category 1"),
                category("Category 2")
        );

        categoryRepository.saveAll(categories);

        productRepository.saveAll(List.of(
                product("Product C", "1000000000000", 30.0, categories.get(0), 0),
                product("Product A", "2000000000000", 60.0, categories.get(1), 10),
                product("Product F", "3000000000000", 10.0, categories.get(0), 20),
                product("Product B", "4000000000000", 50.0, categories.get(1), 30),
                product("Product E", "5000000000000", 20.0, categories.get(0), 40),
                product("Product D", "6000000000000", 40.0, categories.get(1), 50)
        ));
    }

    @Test
    void shouldReturnAllProducts() throws Exception {
        List<Product> initialProducts = productRepository.findAll();

        ResultActions result = mockMvc.perform(get("/admin/products")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Products retrieved successfully"))
                .andExpect(jsonPath("$.data", hasSize(initialProducts.size())));

        for (Product product : initialProducts) {
            String productBySku = "$.data[?(@.ean == '" + product.getEan() + "')]";

            result.andExpect(jsonPath(productBySku, hasSize(1)))
                    .andExpect(jsonPath(productBySku + ".productName",
                            contains(product.getProductName())))
                    .andExpect(jsonPath(productBySku + ".price",
                            contains(product.getPrice())))
                    .andExpect(jsonPath(productBySku + ".stock",
                            contains(product.getStock())))
                    .andExpect(jsonPath(productBySku + ".category.name",
                            contains(product.getCategory().getName())));
        }
    }

    @Test
    void deletedProductShouldNotBeReturnedInAllProducts() throws Exception {
        List<Product> initialProducts = productRepository.findAll();

        Product productToDelete = initialProducts.getFirst();

        String productBySku = "$.data[?(@.ean == '" + productToDelete.getEan() + "')]";

        mockMvc.perform(delete("/admin/products/" + productToDelete.getId())
                        .with(adminJwt()))
                .andExpect(jsonPath("$.status").value(200));

        mockMvc.perform(get("/admin/products")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", hasSize(initialProducts.size() - 1)))
                .andExpect(jsonPath(productBySku).isEmpty());

        assertThat(productRepository.findById(productToDelete.getId()).orElseThrow().isDeleted()).isEqualTo(true);
    }

    @Test
    void shouldReturnPaginatedProductsWithConfigurablePageSize() throws Exception {
        String firstPageResponse = mockMvc.perform(get("/admin/products")
                        .param("page", "0")
                        .param("size", "2")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Products retrieved successfully"))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondPageResponse = mockMvc.perform(get("/admin/products")
                        .param("page", "1")
                        .param("size", "2")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Set<String> firstPageEans = Set.copyOf(
                com.jayway.jsonpath.JsonPath.read(firstPageResponse, "$.data[*].ean"));
        Set<String> secondPageEans = Set.copyOf(
                com.jayway.jsonpath.JsonPath.read(secondPageResponse, "$.data[*].ean"));

        assertThat(firstPageEans).doesNotContainAnyElementsOf(secondPageEans);

        mockMvc.perform(get("/admin/products")
                        .param("page", "0")
                        .param("size", "4")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(4)));
    }

    @Test
    void adminShouldSortProductsByEveryVisibleTableColumn() throws Exception {
        expectProductsSortedBy("name", "asc", "$.data[*].productName",
                "Product A", "Product B", "Product C",
                "Product D", "Product E", "Product F");
        expectProductsSortedBy("ean", "desc", "$.data[*].ean",
                "6000000000000", "5000000000000", "4000000000000",
                "3000000000000", "2000000000000", "1000000000000");
        expectProductsSortedBy("stock", "desc", "$.data[*].stock",
                50, 40, 30, 20, 10, 0);
        expectProductsSortedBy("category", "asc", "$.data[*].ean",
                "1000000000000", "3000000000000", "5000000000000",
                "2000000000000", "4000000000000", "6000000000000");
        expectProductsSortedBy("price", "desc", "$.data[*].price",
                60.0, 50.0, 40.0, 30.0, 20.0, 10.0);
    }

    @Test
    void dateAddedShouldNotBeAvailableAsAProductSortColumn() throws Exception {
        mockMvc.perform(get("/admin/products")
                        .param("sortBy", "dateAdded")
                        .with(adminJwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void adminShouldSearchProductsByNameEanOrCategoryUsingOneQuery() throws Exception {
        expectProductsMatchingQuery("product a", "2000000000000");
        expectProductsMatchingQuery("3000000000000", "3000000000000");
        expectProductsMatchingQuery(
                "CATEGORY 1", "1000000000000", "5000000000000", "3000000000000");
    }

    @Test
    void adminShouldCreateProductWithDetailsCategoryStatusStockAndImages() throws Exception {
        Category category = categoryRepository.findAll().getFirst();
        Map<String, Object> request = validProductRequest(category);
        MockMultipartFile frontImage = image("front.png", "front-image");
        MockMultipartFile backImage = image("back.png", "back-image");

        String response = mockMvc.perform(multipart("/admin/products")
                        .file(productPart(request))
                        .file(frontImage)
                        .file(backImage)
                        .with(adminJwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Product created successfully"))
                .andExpect(jsonPath("$.data.productName").value("New Product"))
                .andExpect(jsonPath("$.data.description").value("A useful product"))
                .andExpect(jsonPath("$.data.price").value(49.99))
                .andExpect(jsonPath("$.data.stock").value(25))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.category.id").value(category.getId()))
                .andExpect(jsonPath("$.data.ean", matchesPattern("\\d{13}")))
                .andExpect(jsonPath("$.data.images", hasSize(2)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<String> imageIds = com.jayway.jsonpath.JsonPath.read(
                response, "$.data.images[*].id");
        assertThat(imageIds)
                .hasSize(2)
                .doesNotHaveDuplicates()
                .allSatisfy(imageId -> assertThat(imageId)
                        .startsWith("products/")
                        .endsWith(".png"));

        var putObjectRequest = org.mockito.ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, times(2)).putObject(
                putObjectRequest.capture(), any(RequestBody.class));
        assertThat(putObjectRequest.getAllValues()).allSatisfy(capturedRequest -> {
            assertThat(capturedRequest.bucket()).isEqualTo("test-product-images");
            assertThat(capturedRequest.contentType()).isEqualTo(MediaType.IMAGE_PNG_VALUE);
            assertThat(capturedRequest.key()).startsWith("products/").endsWith(".png");
        });

        mockMvc.perform(get("/admin/products")
                        .param("query", "New Product")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].productName").value("New Product"));
    }

    @Test
    void productCreationShouldValidateRequiredAndNumericFields() throws Exception {
        Category category = categoryRepository.findAll().getFirst();

        expectProductCreationError(withValue(validProductRequest(category), "productName", ""),
                "Product name is required");
        expectProductCreationError(withValue(validProductRequest(category), "price", 0),
                "Price must be greater than 0");
        expectProductCreationError(withValue(validProductRequest(category), "stock", -1),
                "Stock cannot be negative");
        expectProductCreationError(withValue(validProductRequest(category), "categoryId", ""),
                "Category is required");
        expectProductCreationError(withValue(validProductRequest(category), "status", null),
                "Status is required");
    }

    @Test
    void productCreationShouldPreventDuplicateNamesIgnoringCaseAndWhitespace() throws Exception {
        Category category = categoryRepository.findAll().getFirst();
        Map<String, Object> request = withValue(
                validProductRequest(category), "productName", " product c ");

        mockMvc.perform(multipart("/admin/products")
                        .file(productPart(request))
                        .with(adminJwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("A product with this name already exists"));
    }

    @Test
    void adminShouldRetrieveCategoriesForProductAssignment() throws Exception {
        mockMvc.perform(get("/admin/categories")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].name", containsInAnyOrder("Category 1", "Category 2")));
    }

    @Test
    void nonAdminUserCannotCreateProducts() throws Exception {
        Category category = categoryRepository.findAll().getFirst();

        mockMvc.perform(multipart("/admin/products")
                        .file(productPart(validProductRequest(category)))
                        .with(userJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminShouldRetrieveAProductDetail() throws Exception {
        Product product = productRepository.findAll().getFirst();
        product.setDescription("Detailed description");
        product.setStatus(pl.atakowiec.ecommerce.catalog.model.ProductStatus.DRAFT);
        product.setImages(List.of(new ProductImage(
                "products/current.png", "current.png", MediaType.IMAGE_PNG_VALUE)));
        productRepository.save(product);

        mockMvc.perform(get("/admin/products/{productId}", product.getId())
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Product retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(product.getId()))
                .andExpect(jsonPath("$.data.productName").value(product.getProductName()))
                .andExpect(jsonPath("$.data.description").value("Detailed description"))
                .andExpect(jsonPath("$.data.price").value(product.getPrice()))
                .andExpect(jsonPath("$.data.stock").value(product.getStock()))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.category.id").value(product.getCategory().getId()))
                .andExpect(jsonPath("$.data.images[0].fileName").value("current.png"));
    }

    @Test
    void adminShouldRetrieveTheActualStoredProductImage() throws Exception {
        Product product = productRepository.findAll().getFirst();
        product.setImages(List.of(new ProductImage(
                "products/current.png", "current.png", MediaType.IMAGE_PNG_VALUE)));
        productRepository.save(product);
        byte[] imageBytes = "actual-image-content".getBytes();
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(
                        GetObjectResponse.builder()
                                .contentType(MediaType.IMAGE_PNG_VALUE)
                                .build(),
                        imageBytes));

        mockMvc.perform(get("/admin/products/{productId}/images/{imageIndex}",
                        product.getId(), 0)
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().contentType(MediaType.IMAGE_PNG))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().bytes(imageBytes));

        var getRequest = org.mockito.ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObjectAsBytes(getRequest.capture());
        assertThat(getRequest.getValue().bucket()).isEqualTo("test-product-images");
        assertThat(getRequest.getValue().key()).isEqualTo("products/current.png");
    }

    @Test
    void missingProductImageCannotBeRetrieved() throws Exception {
        Product product = productRepository.findAll().getFirst();

        mockMvc.perform(get("/admin/products/{productId}/images/{imageIndex}",
                        product.getId(), 0)
                        .with(adminJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product image not found"));
    }

    @Test
    void adminShouldUpdateAllProductDetailsReplaceItsImageAndSeeChangesImmediately() throws Exception {
        Product product = productRepository.findAll().getFirst();
        product.setImages(List.of(new ProductImage(
                "products/old.png", "old.png", MediaType.IMAGE_PNG_VALUE)));
        productRepository.save(product);
        Category newCategory = categoryRepository.findAll().stream()
                .filter(category -> !category.getId().equals(product.getCategory().getId()))
                .findFirst()
                .orElseThrow();
        Map<String, Object> request = validUpdateRequest(newCategory);
        MockMultipartFile newImage = image("replacement.webp", "replacement-image", "image/webp");

        mockMvc.perform(multipart("/admin/products/{productId}", product.getId())
                        .file(productPart(request))
                        .file(newImage)
                        .with(updateMethod())
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Product updated successfully"))
                .andExpect(jsonPath("$.data.id").value(product.getId()))
                .andExpect(jsonPath("$.data.productName").value("Updated Product"))
                .andExpect(jsonPath("$.data.description").value("Updated description"))
                .andExpect(jsonPath("$.data.price").value(79.99))
                .andExpect(jsonPath("$.data.stock").value(17))
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"))
                .andExpect(jsonPath("$.data.category.id").value(newCategory.getId()))
                .andExpect(jsonPath("$.data.images", hasSize(1)))
                .andExpect(jsonPath("$.data.images[0].fileName").value("replacement.webp"));

        var deleteRequest = org.mockito.ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(deleteRequest.capture());
        assertThat(deleteRequest.getValue().key()).isEqualTo("products/old.png");

        mockMvc.perform(get("/admin/products/{productId}", product.getId())
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productName").value("Updated Product"))
                .andExpect(jsonPath("$.data.images[0].fileName").value("replacement.webp"));

        mockMvc.perform(get("/admin/products")
                        .param("query", "Updated Product")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(product.getId()))
                .andExpect(jsonPath("$.data[0].price").value(79.99));
    }

    @Test
    void productUpdateShouldRetainCurrentImagesWhenNoReplacementIsUploaded() throws Exception {
        Product product = productRepository.findAll().getFirst();
        product.setImages(List.of(new ProductImage(
                "products/current.png", "current.png", MediaType.IMAGE_PNG_VALUE)));
        productRepository.save(product);

        mockMvc.perform(multipart("/admin/products/{productId}", product.getId())
                        .file(productPart(validUpdateRequest(product.getCategory())))
                        .with(updateMethod())
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.images[0].id").value("products/current.png"));

        verify(s3Client, times(0)).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void productUpdateShouldValidateRequiredAndNumericFields() throws Exception {
        Product product = productRepository.findAll().getFirst();
        Category category = product.getCategory();

        expectProductUpdateError(product, withValue(validUpdateRequest(category), "productName", " "),
                "Product name is required");
        expectProductUpdateError(product, withValue(validUpdateRequest(category), "price", 0),
                "Price must be greater than 0");
        expectProductUpdateError(product, withValue(validUpdateRequest(category), "stock", -1),
                "Stock cannot be negative");
        expectProductUpdateError(product, withValue(validUpdateRequest(category), "stock", 1.5),
                "Stock must be a whole number");
        expectProductUpdateError(product, withValue(validUpdateRequest(category), "categoryId", ""),
                "Category is required");
        expectProductUpdateError(product, withValue(validUpdateRequest(category), "status", null),
                "Status is required");
    }

    @Test
    void productUpdateShouldRejectAnUnknownCategoryAndADuplicateName() throws Exception {
        Product product = productRepository.findAll().getFirst();
        Product anotherProduct = productRepository.findAll().stream()
                .filter(candidate -> !candidate.getId().equals(product.getId()))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(multipart("/admin/products/{productId}", product.getId())
                        .file(productPart(withValue(
                                validUpdateRequest(product.getCategory()),
                                "categoryId", "missing-category")))
                        .with(updateMethod())
                        .with(adminJwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Selected category does not exist"));

        mockMvc.perform(multipart("/admin/products/{productId}", product.getId())
                        .file(productPart(withValue(
                                validUpdateRequest(product.getCategory()),
                                "productName", " " + anotherProduct.getProductName().toLowerCase() + " ")))
                        .with(updateMethod())
                        .with(adminJwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A product with this name already exists"));
    }

    @Test
    void missingProductCannotBeViewedOrUpdated() throws Exception {
        Category category = categoryRepository.findAll().getFirst();

        mockMvc.perform(get("/admin/products/missing-product")
                        .with(adminJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found"));

        mockMvc.perform(multipart("/admin/products/missing-product")
                        .file(productPart(validUpdateRequest(category)))
                        .with(updateMethod())
                        .with(adminJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found"));
    }

    @Test
    void unauthenticatedAndNonAdminUsersCannotViewOrUpdateAProduct() throws Exception {
        Product product = productRepository.findAll().getFirst();

        mockMvc.perform(get("/admin/products/{productId}", product.getId()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(multipart("/admin/products/{productId}", product.getId())
                        .file(productPart(validUpdateRequest(product.getCategory())))
                        .with(updateMethod()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/admin/products/{productId}", product.getId())
                        .with(userJwt()))
                .andExpect(status().isForbidden());
        mockMvc.perform(multipart("/admin/products/{productId}", product.getId())
                        .file(productPart(validUpdateRequest(product.getCategory())))
                        .with(updateMethod())
                        .with(userJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthorizedUserCantGetAllProductsFromAdminController() throws Exception {
        mockMvc.perform(get("/admin/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void nonAdminUserCantGetAllProductsFromAdminController() throws Exception {
        mockMvc.perform(get("/admin/products")
                        .with(userJwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access is denied"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    private Category category(String name) {
        Category category = new Category();
        category.setName(name);

        return category;
    }

    private Product product(String name, String ean, double price, Category category, int stock) {
        return Product.builder()
                .productName(name)
                .ean(ean)
                .price(price)
                .category(category)
                .stock(stock)
                .build();
    }

    private void expectProductsSortedBy(
            String sortBy, String direction, String jsonPathExpression, Object... expectedValues) throws Exception {
        mockMvc.perform(get("/admin/products")
                        .param("sortBy", sortBy)
                        .param("direction", direction)
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(jsonPathExpression, contains(expectedValues)));
    }

    private void expectProductsMatchingQuery(String query, String... expectedEans) throws Exception {
        mockMvc.perform(get("/admin/products")
                        .param("query", query)
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].ean", contains(expectedEans)));
    }

    private Map<String, Object> validProductRequest(Category category) {
        Map<String, Object> request = new HashMap<>();
        request.put("productName", "New Product");
        request.put("description", "A useful product");
        request.put("price", 49.99);
        request.put("categoryId", category.getId());
        request.put("stock", 25);
        request.put("status", "ACTIVE");
        return request;
    }

    private Map<String, Object> validUpdateRequest(Category category) {
        Map<String, Object> request = new HashMap<>();
        request.put("productName", "Updated Product");
        request.put("description", "Updated description");
        request.put("price", 79.99);
        request.put("categoryId", category.getId());
        request.put("stock", 17);
        request.put("status", "ARCHIVED");
        return request;
    }

    private Map<String, Object> withValue(
            Map<String, Object> request, String field, Object value) {
        request.put(field, value);
        return request;
    }

    private MockMultipartFile productPart(Map<String, Object> request) throws Exception {
        return new MockMultipartFile(
                "product",
                "product.json",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request));
    }

    private MockMultipartFile image(String fileName, String content) {
        return image(fileName, content, MediaType.IMAGE_PNG_VALUE);
    }

    private MockMultipartFile image(String fileName, String content, String contentType) {
        return new MockMultipartFile(
                "images", fileName, contentType, content.getBytes());
    }

    private void expectProductCreationError(
            Map<String, Object> request, String expectedMessage) throws Exception {
        mockMvc.perform(multipart("/admin/products")
                        .file(productPart(request))
                        .with(adminJwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    private void expectProductUpdateError(
            Product product, Map<String, Object> request, String expectedMessage) throws Exception {
        mockMvc.perform(multipart("/admin/products/{productId}", product.getId())
                        .file(productPart(request))
                        .with(updateMethod())
                        .with(adminJwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor updateMethod() {
        return request -> {
            request.setMethod("PUT");
            return request;
        };
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor adminJwt() {
        return jwt().authorities(new SimpleGrantedAuthority("SCOPE_ADMIN"));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor userJwt() {
        return jwt().authorities(new SimpleGrantedAuthority("SCOPE_USER"));
    }
}
