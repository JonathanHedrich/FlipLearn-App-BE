package de.fliplearn.backend.study.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record StudySessionResponse(
        Long sessionId,
        Long setId,
        String setTitle,
        OffsetDateTime startedAt,
        int totalCards,
        int correctAnswers,
        int incorrectAnswers,
        boolean completed,
        List<StudyCardResponse> cards
) {
}