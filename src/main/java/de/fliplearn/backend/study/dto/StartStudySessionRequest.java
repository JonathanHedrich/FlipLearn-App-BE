package de.fliplearn.backend.study.dto;

import de.fliplearn.backend.study.entity.StudyMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StartStudySessionRequest(

        @NotNull(message = "Die Lernset-ID ist erforderlich.")
        @Positive(message = "Die Lernset-ID muss positiv sein.")
        Long setId,

        @NotNull(message = "Der Lernmodus ist erforderlich.")
        StudyMode mode,

        Long sourceSessionId

) {
}