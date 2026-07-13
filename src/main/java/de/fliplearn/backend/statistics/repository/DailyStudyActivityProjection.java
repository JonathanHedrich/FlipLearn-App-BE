package de.fliplearn.backend.statistics.repository;

import java.time.LocalDate;

public interface DailyStudyActivityProjection {

    LocalDate getActivityDate();

    Long getReviewCount();

    Long getCorrectCount();
}