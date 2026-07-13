package de.fliplearn.backend.study.dto;

import de.fliplearn.backend.study.entity.StudyRating;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SubmitReviewRequest(

        @NotNull(message = "Die Karten-ID ist erforderlich.")
        @Positive(message = "Die Karten-ID muss positiv sein.")
        Long cardId,

        @NotNull(message = "Eine Bewertung ist erforderlich.")
        StudyRating rating

) {
}