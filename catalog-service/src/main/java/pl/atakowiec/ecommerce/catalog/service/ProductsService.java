package pl.atakowiec.ecommerce.catalog.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.bson.Document;
import pl.atakowiec.ecommerce.catalog.model.Product;
import pl.atakowiec.ecommerce.catalog.repository.ProductRepository;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
@RequiredArgsConstructor
public class ProductsService {
    private final ProductRepository productRepository;
    private final MongoTemplate mongoTemplate;

    public List<Product> findProducts(
            int page, int size, String sortBy, String direction, String query) {
        PageRequest pageRequest = PageRequest.of(page, size);
        ProductSortField sortField = ProductSortField.fromRequestValue(sortBy);
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        String normalizedQuery = query.trim();

        if (sortField.isCategory() || !normalizedQuery.isEmpty()) {
            return findProductsWithAggregation(
                    pageRequest, sortField, sortDirection, normalizedQuery);
        }

        return productRepository.findAll(
                pageRequest.withSort(sortField.toSort(sortDirection))).getContent();
    }

    private List<Product> findProductsWithAggregation(
            PageRequest pageRequest,
            ProductSortField sortField,
            Sort.Direction direction,
            String query) {
        List<AggregationOperation> operations = new ArrayList<>();
        operations.add(lookup("categories", "category", "_id", "resolvedCategory"));
        operations.add(unwind("resolvedCategory"));

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
}
