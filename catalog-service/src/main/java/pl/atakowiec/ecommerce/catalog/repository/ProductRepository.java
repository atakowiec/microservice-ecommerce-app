package pl.atakowiec.ecommerce.catalog.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import pl.atakowiec.ecommerce.catalog.model.Product;

public interface ProductRepository extends MongoRepository<Product, String> {
    boolean existsByProductNameIgnoreCase(String productName);

    boolean existsByProductNameIgnoreCaseAndIdNot(String productName, String id);

    boolean existsByEan(String ean);

    Page<Product> findByDeleted(boolean deleted, PageRequest pageRequest);

    default Page<Product> findNotDeleted(PageRequest pageRequest) {
        return findByDeleted(false, pageRequest);
    }
}
