package de.fliplearn.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(

        @NotBlank(
                message = "Das Google-ID-Token darf nicht leer sein."
        )
        String idToken

) {
}