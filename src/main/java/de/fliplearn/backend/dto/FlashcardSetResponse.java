package de.fliplearn.backend.dto;

import java.time.OffsetDateTime;

public record FlashcardSetResponse(
        Long id,
        String title,
        String description,
        String folder,
        String color,
        boolean favorite,
        int progress,
        int cardCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}