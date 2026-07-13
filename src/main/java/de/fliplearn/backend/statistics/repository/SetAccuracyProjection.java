package de.fliplearn.backend.statistics.repository;

public interface SetAccuracyProjection {

    Long getSetId();

    String getTitle();

    String getColor();

    Long getTotalReviews();

    Long getCorrectReviews();
}