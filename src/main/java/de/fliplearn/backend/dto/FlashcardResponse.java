package de.fliplearn.backend.dto;

import java.time.OffsetDateTime;

public record FlashcardResponse(
        Long id,
        Long setId,
        String front,
        String back,
        boolean favorite,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}