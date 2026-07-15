package de.fliplearn.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateFlashcardSetRequest(

        @NotBlank(message = "Der Titel darf nicht leer sein.")
        @Size(
                min = 2,
                max = 100,
                message = "Der Titel muss zwischen 2 und 100 Zeichen lang sein."
        )
        String title,

        @Size(
                max = 500,
                message = "Die Beschreibung darf höchstens 500 Zeichen lang sein."
        )
        String description,

        Long categoryId,

        @NotBlank(message = "Die Farbe darf nicht leer sein.")
        @Pattern(
                regexp = "blue|purple|green|orange|red|cyan",
                message = "Die Farbe ist ungültig."
        )
        String color

) {
}