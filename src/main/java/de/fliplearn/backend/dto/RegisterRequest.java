package de.fliplearn.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Der Name darf nicht leer sein.")
        @Size(
                min = 2,
                max = 150,
                message = "Der Name muss zwischen 2 und 150 Zeichen lang sein."
        )
        String displayName,

        @NotBlank(message = "Die E-Mail-Adresse darf nicht leer sein.")
        @Email(message = "Die E-Mail-Adresse ist ungültig.")
        @Size(
                max = 255,
                message = "Die E-Mail-Adresse darf höchstens 255 Zeichen lang sein."
        )
        String email,

        @NotBlank(message = "Das Passwort darf nicht leer sein.")
        @Size(
                min = 8,
                max = 72,
                message = "Das Passwort muss zwischen 8 und 72 Zeichen lang sein."
        )
        String password

) {
}