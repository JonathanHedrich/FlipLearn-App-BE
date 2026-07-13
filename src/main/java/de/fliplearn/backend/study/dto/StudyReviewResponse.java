package de.fliplearn.backend.study.dto;

import de.fliplearn.backend.study.entity.StudyRating;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record StudyReviewResponse(
        Long reviewId,
        Long sessionId,
        Long cardId,
        StudyRating rating,
        boolean answeredCorrectly,
        int previousIntervalDays,
        int newIntervalDays,
        BigDecimal previousEaseFactor,
        BigDecimal newEaseFactor,
        OffsetDateTime nextReviewAt,
        boolean sessionComplete,
        int correctAnswers,
        int incorrectAnswers,
        int setProgress
) {
}