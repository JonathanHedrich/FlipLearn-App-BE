package de.fliplearn.backend.dto;

public record GoogleUser(
        String subject,
        String email,
        String displayName,
        String pictureUrl,
        boolean emailVerified
) {
}