package pl.atakowiec.ecommerce.catalog.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.atakowiec.ecommerce.catalog.model.Category;
import pl.atakowiec.ecommerce.catalog.repository.CategoryRepository;
import pl.atakowiec.ecommerce.shared.response.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoriesController {
    private final CategoryRepository categoryRepository;

    @GetMapping
    public ApiResponse<List<Category>> findCategories() {
        return ApiResponse.ok(
                "Categories retrieved successfully", categoryRepository.findAll());
    }
}
