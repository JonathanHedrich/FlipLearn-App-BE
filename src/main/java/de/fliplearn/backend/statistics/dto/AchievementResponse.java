package de.fliplearn.backend.statistics.dto;

public record AchievementResponse(
        String code,
        String title,
        String icon,
        boolean earned,
        long currentValue,
        long targetValue
) {
}