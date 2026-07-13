package de.fliplearn.backend.study.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SchedulingResult(
        BigDecimal easeFactor,
        int intervalDays,
        int repetitions,
        OffsetDateTime nextReviewAt,
        boolean answeredCorrectly
) {
}