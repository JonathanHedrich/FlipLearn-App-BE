package de.fliplearn.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(

        @NotBlank(message = "Der Anzeigename ist erforderlich.")
        @Size(
                min = 2,
                max = 150,
                message = "Der Anzeigename muss zwischen 2 und 150 Zeichen lang sein."
        )
        String displayName,

        @NotBlank(message = "Der Benutzername ist erforderlich.")
        @Size(
                min = 3,
                max = 100,
                message = "Der Benutzername muss zwischen 3 und 100 Zeichen lang sein."
        )
        String username

) {
}