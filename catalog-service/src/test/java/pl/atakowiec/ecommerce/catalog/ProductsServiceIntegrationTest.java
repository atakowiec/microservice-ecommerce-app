package pl.atakowiec.ecommerce.catalog;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import pl.atakowiec.ecommerce.catalog.model.Category;
import pl.atakowiec.ecommerce.catalog.model.Product;
import pl.atakowiec.ecommerce.catalog.repository.CategoryRepository;
import pl.atakowiec.ecommerce.catalog.repository.ProductRepository;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest()
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

    private static org.springframework.test.web.servlet.request.RequestPostProcessor adminJwt() {
        return jwt().authorities(new SimpleGrantedAuthority("SCOPE_ADMIN"));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor userJwt() {
        return jwt().authorities(new SimpleGrantedAuthority("SCOPE_USER"));
    }
}
