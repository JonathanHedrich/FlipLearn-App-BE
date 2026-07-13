package de.fliplearn.backend.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Long userId,
        String displayName,
        String username,
        String email,
        String role
) {
}