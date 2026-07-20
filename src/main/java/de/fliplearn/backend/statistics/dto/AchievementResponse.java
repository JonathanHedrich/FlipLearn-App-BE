package de.fliplearn.backend.statistics.dto;

import java.util.Map;

public record AchievementResponse(
        String code,
        String icon,
        boolean earned,
        long currentValue,
        long targetValue,
        int progress,
        Map<String, Object> params
) {
}