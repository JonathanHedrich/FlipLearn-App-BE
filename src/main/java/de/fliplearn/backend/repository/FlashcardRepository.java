package de.fliplearn.backend.repository;

import de.fliplearn.backend.entity.Flashcard;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface FlashcardRepository
        extends JpaRepository<Flashcard, Long> {

    List<Flashcard>
    findAllByFlashcardSetIdOrderByCreatedAtAsc(
            Long setId
    );

    Optional<Flashcard>
    findByIdAndFlashcardSetIdAndFlashcardSetOwnerEmailIgnoreCase(
            Long cardId,
            Long setId,
            String email
    );

    List<Flashcard>
    findAllByFlashcardSetIdAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc(
            Long setId,
            OffsetDateTime date
    );

    List<Flashcard>
    findAllByFlashcardSetIdAndNextReviewAtIsNullOrderByCreatedAtAsc(
            Long setId
    );

    long countByFlashcardSetOwnerEmailIgnoreCase(
            String email
    );

    @Query("""
        select coalesce(sum(card.totalReviews), 0)
        from Flashcard card
        where lower(card.flashcardSet.owner.email) = lower(:email)
        """)
    long sumTotalReviewsByOwnerEmail(
            @Param("email") String email
    );

    @Query("""
        select coalesce(sum(card.correctReviews), 0)
        from Flashcard card
        where lower(card.flashcardSet.owner.email) = lower(:email)
        """)
    long sumCorrectReviewsByOwnerEmail(
            @Param("email") String email
    );
}