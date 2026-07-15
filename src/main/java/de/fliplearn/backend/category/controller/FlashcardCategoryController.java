package de.fliplearn.backend.category.controller;

import de.fliplearn.backend.category.dto.CategoryRequest;
import de.fliplearn.backend.category.dto.CategoryResponse;
import de.fliplearn.backend.category.service.FlashcardCategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class FlashcardCategoryController {

    private final FlashcardCategoryService categoryService;

    public FlashcardCategoryController(
            FlashcardCategoryService categoryService
    ) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryResponse> getCategories(
            Authentication authentication
    ) {
        return categoryService.getCategories(
                authentication.getName()
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(
            Authentication authentication,
            @Valid @RequestBody CategoryRequest request
    ) {
        return categoryService.createCategory(
                authentication.getName(),
                request
        );
    }

    @PutMapping("/{categoryId}")
    public CategoryResponse updateCategory(
            Authentication authentication,
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryRequest request
    ) {
        return categoryService.updateCategory(
                authentication.getName(),
                categoryId,
                request
        );
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(
            Authentication authentication,
            @PathVariable Long categoryId
    ) {
        categoryService.deleteCategory(
                authentication.getName(),
                categoryId
        );
    }
}