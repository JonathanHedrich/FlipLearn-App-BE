package de.fliplearn.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFlashcardRequest(

        @NotBlank(message = "Die Vorderseite darf nicht leer sein.")
        @Size(
                max = 5000,
                message = "Die Vorderseite darf höchstens 5000 Zeichen enthalten."
        )
        String front,

        @NotBlank(message = "Die Rückseite darf nicht leer sein.")
        @Size(
                max = 5000,
                message = "Die Rückseite darf höchstens 5000 Zeichen enthalten."
        )
        String back

) {
}