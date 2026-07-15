package de.fliplearn.backend.category.repository;

import de.fliplearn.backend.category.entity.FlashcardCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FlashcardCategoryRepository
        extends JpaRepository<FlashcardCategory, Long> {

    List<FlashcardCategory>
    findAllByOwnerEmailIgnoreCaseOrderByNameAsc(
            String email
    );

    Optional<FlashcardCategory>
    findByIdAndOwnerEmailIgnoreCase(
            Long categoryId,
            String email
    );

    boolean existsByOwnerEmailIgnoreCaseAndNameIgnoreCase(
            String email,
            String name
    );
}