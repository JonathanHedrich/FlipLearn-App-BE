package de.fliplearn.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(

        @NotBlank(message = "Das aktuelle Passwort ist erforderlich.")
        String currentPassword,

        @NotBlank(message = "Das neue Passwort ist erforderlich.")
        @Size(
                min = 8,
                max = 72,
                message = "Das neue Passwort muss zwischen 8 und 72 Zeichen lang sein."
        )
        String newPassword

) {
}