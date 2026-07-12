package de.fliplearn.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "Die E-Mail-Adresse darf nicht leer sein.")
        @Email(message = "Die E-Mail-Adresse ist ungültig.")
        String email,

        @NotBlank(message = "Das Passwort darf nicht leer sein.")
        String password

) {
}