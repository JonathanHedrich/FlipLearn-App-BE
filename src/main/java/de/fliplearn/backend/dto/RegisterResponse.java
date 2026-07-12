package de.fliplearn.backend.dto;

import java.time.OffsetDateTime;

public record RegisterResponse(
        Long id,
        String displayName,
        String email,
        String role,
        OffsetDateTime createdAt
) {
}