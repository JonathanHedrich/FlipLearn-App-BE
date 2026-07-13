package de.fliplearn.backend.statistics.dto;

public record SetAccuracyResponse(
        Long setId,
        String title,
        String color,
        long totalReviews,
        long correctReviews,
        int accuracy
) {
}