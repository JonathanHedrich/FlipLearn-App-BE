package de.fliplearn.backend.dto;

import java.time.OffsetDateTime;

public record CurrentUserResponse(
        Long id,
        String displayName,
        String email,
        String role,
        boolean enabled,
        OffsetDateTime createdAt
) {
}