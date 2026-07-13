package de.fliplearn.backend.study.service;

import de.fliplearn.backend.entity.Flashcard;
import de.fliplearn.backend.study.entity.StudyRating;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

@Service
public class SpacedRepetitionService {

    private static final BigDecimal MIN_EASE_FACTOR =
            new BigDecimal("1.30");

    private static final BigDecimal HARD_EASE_CHANGE =
            new BigDecimal("-0.15");

    private static final BigDecimal EASY_EASE_CHANGE =
            new BigDecimal("0.15");

    public SchedulingResult calculate(
            Flashcard card,
            StudyRating rating,
            OffsetDateTime reviewedAt
    ) {
        BigDecimal previousEaseFactor =
                card.getEaseFactor();

        int previousInterval =
                card.getIntervalDays();

        int previousRepetitions =
                card.getRepetitions();

        return switch (rating) {
            case AGAIN -> calculateAgain(
                    previousEaseFactor,
                    reviewedAt
            );

            case HARD -> calculateHard(
                    previousEaseFactor,
                    previousInterval,
                    previousRepetitions,
                    reviewedAt
            );

            case GOOD -> calculateGood(
                    previousEaseFactor,
                    previousInterval,
                    previousRepetitions,
                    reviewedAt
            );

            case EASY -> calculateEasy(
                    previousEaseFactor,
                    previousInterval,
                    previousRepetitions,
                    reviewedAt
            );
        };
    }

    private SchedulingResult calculateAgain(
            BigDecimal easeFactor,
            OffsetDateTime reviewedAt
    ) {
        BigDecimal newEaseFactor = clampEaseFactor(
                easeFactor.subtract(
                        new BigDecimal("0.20")
                )
        );

        return new SchedulingResult(
                newEaseFactor,
                0,
                0,
                reviewedAt.plusMinutes(1),
                false
        );
    }

    private SchedulingResult calculateHard(
            BigDecimal easeFactor,
            int previousInterval,
            int previousRepetitions,
            OffsetDateTime reviewedAt
    ) {
        BigDecimal newEaseFactor = clampEaseFactor(
                easeFactor.add(HARD_EASE_CHANGE)
        );

        int newInterval = Math.max(
                1,
                Math.round(
                        Math.max(previousInterval, 1) * 1.2f
                )
        );

        return new SchedulingResult(
                newEaseFactor,
                newInterval,
                Math.max(1, previousRepetitions),
                reviewedAt.plusDays(newInterval),
                false
        );
    }

    private SchedulingResult calculateGood(
            BigDecimal easeFactor,
            int previousInterval,
            int previousRepetitions,
            OffsetDateTime reviewedAt
    ) {
        int newRepetitions = previousRepetitions + 1;
        int newInterval;

        if (newRepetitions == 1) {
            newInterval = 1;
        } else if (newRepetitions == 2) {
            newInterval = 6;
        } else {
            newInterval = Math.max(
                    1,
                    easeFactor
                            .multiply(
                                    BigDecimal.valueOf(
                                            Math.max(previousInterval, 1)
                                    )
                            )
                            .setScale(
                                    0,
                                    RoundingMode.HALF_UP
                            )
                            .intValue()
            );
        }

        return new SchedulingResult(
                easeFactor,
                newInterval,
                newRepetitions,
                reviewedAt.plusDays(newInterval),
                true
        );
    }

    private SchedulingResult calculateEasy(
            BigDecimal easeFactor,
            int previousInterval,
            int previousRepetitions,
            OffsetDateTime reviewedAt
    ) {
        BigDecimal newEaseFactor = clampEaseFactor(
                easeFactor.add(EASY_EASE_CHANGE)
        );

        int newRepetitions = previousRepetitions + 1;
        int newInterval;

        if (newRepetitions == 1) {
            newInterval = 4;
        } else {
            newInterval = Math.max(
                    4,
                    newEaseFactor
                            .multiply(
                                    BigDecimal.valueOf(
                                            Math.max(previousInterval, 1)
                                    )
                            )
                            .multiply(new BigDecimal("1.30"))
                            .setScale(
                                    0,
                                    RoundingMode.HALF_UP
                            )
                            .intValue()
            );
        }

        return new SchedulingResult(
                newEaseFactor,
                newInterval,
                newRepetitions,
                reviewedAt.plusDays(newInterval),
                true
        );
    }

    private BigDecimal clampEaseFactor(
            BigDecimal easeFactor
    ) {
        return easeFactor
                .max(MIN_EASE_FACTOR)
                .setScale(2, RoundingMode.HALF_UP);
    }
}