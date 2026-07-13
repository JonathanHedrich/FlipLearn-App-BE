package de.fliplearn.backend.repository;

import de.fliplearn.backend.entity.FlashcardSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FlashcardSetRepository
        extends JpaRepository<FlashcardSet, Long> {

    List<FlashcardSet> findAllByOwnerEmailIgnoreCaseOrderByUpdatedAtDesc(
            String email
    );

    Optional<FlashcardSet> findByIdAndOwnerEmailIgnoreCase(
            Long id,
            String email
    );

    long countByOwnerEmailIgnoreCase(
            String email
    );

    long countByOwnerEmailIgnoreCaseAndFavoriteTrue(
            String email
    );
}