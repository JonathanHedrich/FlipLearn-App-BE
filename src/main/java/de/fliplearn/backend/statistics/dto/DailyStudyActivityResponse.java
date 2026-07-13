package de.fliplearn.backend.statistics.dto;

import java.time.LocalDate;

public record DailyStudyActivityResponse(
        LocalDate date,
        long reviews,
        long correctReviews,
        int accuracy
) {
}