package de.fliplearn.backend.statistics.dto;

import java.util.List;

public record StatisticsOverviewResponse(
        long totalSets,
        long totalCards,
        long completedSessions,
        long totalReviews,
        long correctReviews,
        long incorrectReviews,
        int accuracy,
        long reviewsToday,
        long correctReviewsToday,
        int todayAccuracy,
        List<DailyStudyActivityResponse> lastSevenDays
) {
}