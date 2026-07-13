package de.fliplearn.backend.statistics.service;

import de.fliplearn.backend.repository.FlashcardRepository;
import de.fliplearn.backend.repository.FlashcardSetRepository;
import de.fliplearn.backend.statistics.dto.DailyStudyActivityResponse;
import de.fliplearn.backend.statistics.dto.StatisticsOverviewResponse;
import de.fliplearn.backend.statistics.repository.DailyStudyActivityProjection;
import de.fliplearn.backend.study.repository.StudyReviewRepository;
import de.fliplearn.backend.study.repository.StudySessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatisticsService {

    private final FlashcardSetRepository flashcardSetRepository;
    private final FlashcardRepository flashcardRepository;
    private final StudySessionRepository studySessionRepository;
    private final StudyReviewRepository studyReviewRepository;

    public StatisticsService(
            FlashcardSetRepository flashcardSetRepository,
            FlashcardRepository flashcardRepository,
            StudySessionRepository studySessionRepository,
            StudyReviewRepository studyReviewRepository
    ) {
        this.flashcardSetRepository = flashcardSetRepository;
        this.flashcardRepository = flashcardRepository;
        this.studySessionRepository = studySessionRepository;
        this.studyReviewRepository = studyReviewRepository;
    }

    @Transactional(readOnly = true)
    public StatisticsOverviewResponse getOverview(
            String email
    ) {
        long totalSets =
                flashcardSetRepository
                        .countByOwnerEmailIgnoreCase(email);

        long totalCards =
                flashcardRepository
                        .countByFlashcardSetOwnerEmailIgnoreCase(email);

        long completedSessions =
                studySessionRepository
                        .countByUserEmailIgnoreCaseAndCompletedAtIsNotNull(
                                email
                        );

        long totalReviews =
                studyReviewRepository
                        .countBySessionUserEmailIgnoreCase(email);

        long correctReviews =
                studyReviewRepository
                        .countBySessionUserEmailIgnoreCaseAndAnsweredCorrectlyTrue(
                                email
                        );

        long incorrectReviews =
                Math.max(
                        0,
                        totalReviews - correctReviews
                );

        int accuracy =
                calculateAccuracy(
                        correctReviews,
                        totalReviews
                );

        ZoneId zoneId = ZoneId.systemDefault();

        LocalDate today =
                LocalDate.now(zoneId);

        OffsetDateTime todayStart =
                today
                        .atStartOfDay(zoneId)
                        .toOffsetDateTime();

        OffsetDateTime tomorrowStart =
                today
                        .plusDays(1)
                        .atStartOfDay(zoneId)
                        .toOffsetDateTime();

        long reviewsToday =
                studyReviewRepository
                        .countBySessionUserEmailIgnoreCaseAndReviewedAtBetween(
                                email,
                                todayStart,
                                tomorrowStart
                        );

        long correctReviewsToday =
                studyReviewRepository
                        .countBySessionUserEmailIgnoreCaseAndAnsweredCorrectlyTrueAndReviewedAtBetween(
                                email,
                                todayStart,
                                tomorrowStart
                        );

        int todayAccuracy =
                calculateAccuracy(
                        correctReviewsToday,
                        reviewsToday
                );

        LocalDate firstDay =
                today.minusDays(6);

        OffsetDateTime rangeStart =
                firstDay
                        .atStartOfDay(zoneId)
                        .toOffsetDateTime();

        OffsetDateTime rangeEnd =
                tomorrowStart;

        List<DailyStudyActivityProjection> projections =
                studyReviewRepository.findDailyActivity(
                        email,
                        rangeStart,
                        rangeEnd
                );

        List<DailyStudyActivityResponse> lastSevenDays =
                buildLastSevenDays(
                        firstDay,
                        today,
                        projections
                );

        return new StatisticsOverviewResponse(
                totalSets,
                totalCards,
                completedSessions,
                totalReviews,
                correctReviews,
                incorrectReviews,
                accuracy,
                reviewsToday,
                correctReviewsToday,
                todayAccuracy,
                lastSevenDays
        );
    }

    private List<DailyStudyActivityResponse> buildLastSevenDays(
            LocalDate firstDay,
            LocalDate lastDay,
            List<DailyStudyActivityProjection> projections
    ) {
        Map<LocalDate, DailyStudyActivityProjection> activityByDate =
                new LinkedHashMap<>();

        for (
                DailyStudyActivityProjection projection
                : projections
        ) {
            activityByDate.put(
                    projection.getActivityDate(),
                    projection
            );
        }

        return firstDay
                .datesUntil(lastDay.plusDays(1))
                .map(date -> {
                    DailyStudyActivityProjection activity =
                            activityByDate.get(date);

                    long reviews =
                            activity == null ||
                                    activity.getReviewCount() == null
                                    ? 0
                                    : activity.getReviewCount();

                    long correct =
                            activity == null ||
                                    activity.getCorrectCount() == null
                                    ? 0
                                    : activity.getCorrectCount();

                    return new DailyStudyActivityResponse(
                            date,
                            reviews,
                            correct,
                            calculateAccuracy(
                                    correct,
                                    reviews
                            )
                    );
                })
                .toList();
    }

    private int calculateAccuracy(
            long correct,
            long total
    ) {
        if (total <= 0) {
            return 0;
        }

        return Math.round(
                correct * 100.0f / total
        );
    }
}