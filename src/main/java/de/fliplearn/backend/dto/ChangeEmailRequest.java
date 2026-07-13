package de.fliplearn.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeEmailRequest(

        @NotBlank(message = "Die neue E-Mail-Adresse ist erforderlich.")
        @Email(message = "Die neue E-Mail-Adresse ist ungültig.")
        @Size(
                max = 255,
                message = "Die E-Mail-Adresse darf maximal 255 Zeichen lang sein."
        )
        String newEmail,

        @NotBlank(message = "Das aktuelle Passwort ist erforderlich.")
        String currentPassword

) {
}