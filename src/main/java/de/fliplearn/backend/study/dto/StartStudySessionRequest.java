package de.fliplearn.backend.study.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StartStudySessionRequest(

        @NotNull(message = "Die Lernset-ID ist erforderlich.")
        @Positive(message = "Die Lernset-ID muss positiv sein.")
        Long setId

) {
}