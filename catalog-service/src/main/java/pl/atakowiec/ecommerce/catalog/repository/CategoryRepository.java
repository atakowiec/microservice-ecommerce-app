package pl.atakowiec.ecommerce.catalog.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import pl.atakowiec.ecommerce.catalog.model.Category;

public interface CategoryRepository extends MongoRepository<Category, String> {

}
