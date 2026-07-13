package de.fliplearn.backend.dto;

import java.time.OffsetDateTime;

public record UserProfileResponse(
        Long id,
        String displayName,
        String email,
        String role,
        OffsetDateTime memberSince,
        long totalSets,
        long totalCards,
        long favoriteSets,
        long completedSessions,
        long totalReviews,
        long correctReviews,
        int accuracy
) {
}