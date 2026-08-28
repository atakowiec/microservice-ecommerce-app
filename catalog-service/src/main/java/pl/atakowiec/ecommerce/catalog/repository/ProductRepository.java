package pl.atakowiec.ecommerce.catalog.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import pl.atakowiec.ecommerce.catalog.model.Product;

public interface ProductRepository extends MongoRepository<Product, String> {

}
