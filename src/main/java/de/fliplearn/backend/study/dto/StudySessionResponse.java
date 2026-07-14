package de.fliplearn.backend.study.dto;

import de.fliplearn.backend.study.entity.StudyMode;

import java.time.OffsetDateTime;
import java.util.List;

public record StudySessionResponse(
        Long sessionId,
        Long setId,
        String setTitle,
        StudyMode mode,
        OffsetDateTime startedAt,
        int totalCards,
        int correctAnswers,
        int incorrectAnswers,
        boolean complete,
        List<StudyCardResponse> cards
) {
}