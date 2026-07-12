package de.fliplearn.backend.repository;

import de.fliplearn.backend.entity.Flashcard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FlashcardRepository
        extends JpaRepository<Flashcard, Long> {

    List<Flashcard> findAllByFlashcardSetIdOrderByCreatedAtAsc(
            Long setId
    );

    Optional<Flashcard>
    findByIdAndFlashcardSetIdAndFlashcardSetOwnerEmailIgnoreCase(
            Long cardId,
            Long setId,
            String email
    );
}