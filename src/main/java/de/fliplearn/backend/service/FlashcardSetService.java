package de.fliplearn.backend.service;

import de.fliplearn.backend.dto.CreateFlashcardSetRequest;
import de.fliplearn.backend.dto.FlashcardSetResponse;
import de.fliplearn.backend.entity.AppUser;
import de.fliplearn.backend.entity.FlashcardSet;
import de.fliplearn.backend.exception.ResourceNotFoundException;
import de.fliplearn.backend.repository.AppUserRepository;
import de.fliplearn.backend.repository.FlashcardSetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import de.fliplearn.backend.dto.UpdateFlashcardSetRequest;

import de.fliplearn.backend.category.entity.FlashcardCategory;
import de.fliplearn.backend.category.repository.FlashcardCategoryRepository;

import java.util.List;

@Service
public class FlashcardSetService {

    private final FlashcardSetRepository flashcardSetRepository;
    private final AppUserRepository appUserRepository;
    private final FlashcardCategoryRepository categoryRepository;

    public FlashcardSetService(
            FlashcardSetRepository flashcardSetRepository,
            AppUserRepository appUserRepository,
            FlashcardCategoryRepository categoryRepository
    ) {
        this.flashcardSetRepository = flashcardSetRepository;
        this.appUserRepository = appUserRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<FlashcardSetResponse> getAllSets(
            String email
    ) {
        return flashcardSetRepository
                .findAllByOwnerEmailIgnoreCaseOrderByUpdatedAtDesc(
                        email
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FlashcardSetResponse getSet(
            Long setId,
            String email
    ) {
        FlashcardSet set = findOwnedSet(
                setId,
                email
        );

        return toResponse(set);
    }

    @Transactional
    public FlashcardSetResponse createSet(
            CreateFlashcardSetRequest request,
            String email
    ) {
        AppUser owner = appUserRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Benutzer wurde nicht gefunden."
                        )
                );

        FlashcardSet set = new FlashcardSet(
                owner,
                request.title().trim(),
                request.description(),
                request.color()
        );

        set.setCategory(
                resolveCategory(
                        email,
                        request.categoryId()
                )
        );

        FlashcardSet savedSet =
                flashcardSetRepository.save(set);

        return toResponse(savedSet);
    }

    private FlashcardSet findOwnedSet(
            Long setId,
            String email
    ) {
        return flashcardSetRepository
                .findByIdAndOwnerEmailIgnoreCase(
                        setId,
                        email
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Lernset wurde nicht gefunden."
                        )
                );
    }

    private FlashcardSetResponse toResponse(
            FlashcardSet set
    ) {
        FlashcardCategory category = set.getCategory();

        return new FlashcardSetResponse(
                set.getId(),
                set.getTitle(),
                set.getDescription(),
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,
                set.getColor(),
                set.isFavorite(),
                set.getProgress(),
                set.getCardCount(),
                set.getCreatedAt(),
                set.getUpdatedAt()
        );
    }

    private String normalizeNullable(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    @Transactional
    public FlashcardSetResponse updateSet(
            Long setId,
            UpdateFlashcardSetRequest request,
            String email
    ) {
        FlashcardSet set = findOwnedSet(setId, email);

        set.setTitle(request.title().trim());
        set.setDescription(
                normalizeNullable(request.description())
        );
        set.setCategory(
                resolveCategory(
                        email,
                        request.categoryId()
                )
        );
        set.setColor(request.color());
        set.setFavorite(request.favorite());

        FlashcardSet savedSet =
                flashcardSetRepository.save(set);

        return toResponse(savedSet);
    }

    @Transactional
    public void deleteSet(
            Long setId,
            String email
    ) {
        FlashcardSet set = findOwnedSet(setId, email);

        flashcardSetRepository.delete(set);
    }

    private FlashcardCategory resolveCategory(
            String email,
            Long categoryId
    ) {
        if (categoryId == null) {
            return null;
        }

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
}