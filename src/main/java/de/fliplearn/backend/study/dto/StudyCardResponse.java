package de.fliplearn.backend.study.dto;

import java.time.OffsetDateTime;

public record StudyCardResponse(
        Long id,
        String front,
        String back,
        boolean favorite,
        int intervalDays,
        int repetitions,
        OffsetDateTime nextReviewAt
) {
}