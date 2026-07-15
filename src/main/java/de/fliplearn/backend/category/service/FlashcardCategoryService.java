package de.fliplearn.backend.category.service;

import de.fliplearn.backend.category.dto.CategoryRequest;
import de.fliplearn.backend.category.dto.CategoryResponse;
import de.fliplearn.backend.category.entity.FlashcardCategory;
import de.fliplearn.backend.category.repository.FlashcardCategoryRepository;
import de.fliplearn.backend.entity.AppUser;
import de.fliplearn.backend.exception.ResourceConflictException;
import de.fliplearn.backend.exception.ResourceNotFoundException;
import de.fliplearn.backend.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FlashcardCategoryService {

    private final FlashcardCategoryRepository categoryRepository;
    private final AppUserRepository appUserRepository;

    public FlashcardCategoryService(
            FlashcardCategoryRepository categoryRepository,
            AppUserRepository appUserRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(
            String email
    ) {
        return categoryRepository
                .findAllByOwnerEmailIgnoreCaseOrderByNameAsc(email)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse createCategory(
            String email,
            CategoryRequest request
    ) {
        String name = request.name().trim();

        if (
                categoryRepository
                        .existsByOwnerEmailIgnoreCaseAndNameIgnoreCase(
                                email,
                                name
                        )
        ) {
            throw new ResourceConflictException(
                    "Diese Kategorie existiert bereits."
            );
        }

        AppUser user = appUserRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Benutzer wurde nicht gefunden."
                        )
                );

        FlashcardCategory category =
                new FlashcardCategory(
                        user,
                        name
                );

        return toResponse(
                categoryRepository.save(category)
        );
    }

    @Transactional
    public CategoryResponse updateCategory(
            String email,
            Long categoryId,
            CategoryRequest request
    ) {
        FlashcardCategory category =
                getOwnedCategory(
                        email,
                        categoryId
                );

        String name = request.name().trim();

        boolean duplicate =
                categoryRepository
                        .existsByOwnerEmailIgnoreCaseAndNameIgnoreCase(
                                email,
                                name
                        );

        if (
                duplicate &&
                        !category.getName()
                                .equalsIgnoreCase(name)
        ) {
            throw new ResourceConflictException(
                    "Diese Kategorie existiert bereits."
            );
        }

        category.setName(name);

        return toResponse(category);
    }

    @Transactional
    public void deleteCategory(
            String email,
            Long categoryId
    ) {
        FlashcardCategory category =
                getOwnedCategory(
                        email,
                        categoryId
                );

        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public FlashcardCategory getOwnedCategory(
            String email,
            Long categoryId
    ) {
        return categoryRepository
                .findByIdAndOwnerEmailIgnoreCase(
                        categoryId,
                        email
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Kategorie wurde nicht gefunden."
                        )
                );
    }

    private CategoryResponse toResponse(
            FlashcardCategory category
    ) {
        return new CategoryResponse(
                category.getId(),
                category.getName()
        );
    }
}