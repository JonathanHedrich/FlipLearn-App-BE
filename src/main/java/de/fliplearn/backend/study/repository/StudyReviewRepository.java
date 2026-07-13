package de.fliplearn.backend.study.repository;

import de.fliplearn.backend.study.entity.StudyReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyReviewRepository
        extends JpaRepository<StudyReview, Long> {

    List<StudyReview>
    findAllBySessionIdOrderByReviewedAtAsc(
            Long sessionId
    );

    boolean existsBySessionIdAndCardId(
            Long sessionId,
            Long cardId
    );
}