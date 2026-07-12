package de.fliplearn.backend.repository;

import de.fliplearn.backend.entity.Flashcard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlashcardRepository
        extends JpaRepository<Flashcard, Long> {
}