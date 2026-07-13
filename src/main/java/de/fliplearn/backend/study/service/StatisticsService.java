package de.fliplearn.backend.statistics.service;

import de.fliplearn.backend.repository.FlashcardRepository;
import de.fliplearn.backend.repository.FlashcardSetRepository;
import de.fliplearn.backend.statistics.dto.AchievementResponse;
import de.fliplearn.backend.statistics.dto.DailyStudyActivityResponse;
import de.fliplearn.backend.statistics.dto.SetAccuracyResponse;
import de.fliplearn.backend.statistics.dto.StatisticsOverviewResponse;
import de.fliplearn.backend.statistics.repository.DailyStudyActivityProjection;
import de.fliplearn.backend.statistics.repository.SetAccuracyProjection;
import de.fliplearn.backend.study.repository.StudyReviewRepository;
import de.fliplearn.backend.study.repository.StudySessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
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
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zoneId);

        OffsetDateTime todayStart =
                today
                        .atStartOfDay(zoneId)
                        .toOffsetDateTime();

        OffsetDateTime tomorrowStart =
                today
                        .plusDays(1)
                        .atStartOfDay(zoneId)
                        .toOffsetDateTime();

        /*
         * Aktuelle Kalenderwoche:
         * Montag bis Sonntag.
         */
        LocalDate weekStartDate =
                today.with(
                        TemporalAdjusters.previousOrSame(
                                DayOfWeek.MONDAY
                        )
                );

        LocalDate weekEndDate =
                weekStartDate.plusDays(6);

        OffsetDateTime weekStart =
                weekStartDate
                        .atStartOfDay(zoneId)
                        .toOffsetDateTime();

        OffsetDateTime weekEndExclusive =
                weekStartDate
                        .plusDays(7)
                        .atStartOfDay(zoneId)
                        .toOffsetDateTime();

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

        long reviewsThisWeek =
                studyReviewRepository
                        .countBySessionUserEmailIgnoreCaseAndReviewedAtBetween(
                                email,
                                weekStart,
                                weekEndExclusive
                        );

        long correctReviewsThisWeek =
                studyReviewRepository
                        .countBySessionUserEmailIgnoreCaseAndAnsweredCorrectlyTrueAndReviewedAtBetween(
                                email,
                                weekStart,
                                weekEndExclusive
                        );

        int weeklyAccuracy =
                calculateAccuracy(
                        correctReviewsThisWeek,
                        reviewsThisWeek
                );

        double weeklyStudySeconds =
                studySessionRepository
                        .sumCompletedStudySeconds(
                                email,
                                weekStart,
                                weekEndExclusive
                        );

        long weeklyStudyMinutes =
                Math.round(
                        weeklyStudySeconds / 60.0
                );

        /*
         * Liniendiagramm:
         * Immer Montag bis Sonntag.
         */
        List<DailyStudyActivityProjection> chartProjections =
                studyReviewRepository.findDailyActivity(
                        email,
                        weekStart,
                        weekEndExclusive
                );

        List<DailyStudyActivityResponse> lastSevenDays =
                buildDailyActivity(
                        weekStartDate,
                        weekEndDate,
                        chartProjections
                );

        /*
         * Streak wird aus maximal 365 Tagen berechnet.
         */
        OffsetDateTime streakStart =
                today
                        .minusDays(365)
                        .atStartOfDay(zoneId)
                        .toOffsetDateTime();

        List<DailyStudyActivityProjection> streakProjections =
                studyReviewRepository.findDailyActivity(
                        email,
                        streakStart,
                        tomorrowStart
                );

        int currentStreak =
                calculateCurrentStreak(
                        today,
                        streakProjections
                );

        /*
         * Kalender:
         * Ebenfalls Montag bis Sonntag.
         */
        List<DailyStudyActivityProjection> calendarProjections =
                studyReviewRepository.findDailyActivity(
                        email,
                        weekStart,
                        weekEndExclusive
                );

        List<DailyStudyActivityResponse> studyCalendar =
                buildDailyActivity(
                        weekStartDate,
                        weekEndDate,
                        calendarProjections
                );

        List<SetAccuracyResponse> setAccuracies =
                buildSetAccuracies(
                        studyReviewRepository
                                .findSetAccuracies(email)
                );

        List<AchievementResponse> achievements =
                buildAchievements(
                        totalSets,
                        totalCards,
                        completedSessions,
                        totalReviews,
                        accuracy,
                        currentStreak,
                        weeklyStudyMinutes
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

                reviewsThisWeek,
                correctReviewsThisWeek,
                weeklyAccuracy,
                weeklyStudyMinutes,
                currentStreak,

                lastSevenDays,
                studyCalendar,
                setAccuracies,
                achievements
        );
    }

    private List<DailyStudyActivityResponse> buildDailyActivity(
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

    private int calculateCurrentStreak(
            LocalDate today,
            List<DailyStudyActivityProjection> projections
    ) {
        Map<LocalDate, Long> reviewsByDate =
                new LinkedHashMap<>();

        for (
                DailyStudyActivityProjection projection
                : projections
        ) {
            reviewsByDate.put(
                    projection.getActivityDate(),
                    projection.getReviewCount() == null
                            ? 0
                            : projection.getReviewCount()
            );
        }

        /*
         * Falls heute noch nicht gelernt wurde,
         * darf eine bis gestern bestehende Serie erhalten bleiben.
         */
        LocalDate currentDate =
                reviewsByDate.getOrDefault(today, 0L) > 0
                        ? today
                        : today.minusDays(1);

        int streak = 0;

        while (
                reviewsByDate.getOrDefault(
                        currentDate,
                        0L
                ) > 0
        ) {
            streak++;
            currentDate = currentDate.minusDays(1);
        }

        return streak;
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

    private List<SetAccuracyResponse> buildSetAccuracies(
            List<SetAccuracyProjection> projections
    ) {
        return projections
                .stream()
                .map(projection -> {
                    long totalReviews =
                            projection.getTotalReviews() == null
                                    ? 0
                                    : projection.getTotalReviews();

                    long correctReviews =
                            projection.getCorrectReviews() == null
                                    ? 0
                                    : projection.getCorrectReviews();

                    return new SetAccuracyResponse(
                            projection.getSetId(),
                            projection.getTitle(),
                            projection.getColor(),
                            totalReviews,
                            correctReviews,
                            calculateAccuracy(
                                    correctReviews,
                                    totalReviews
                            )
                    );
                })
                .toList();
    }

    private List<AchievementResponse> buildAchievements(
            long totalSets,
            long totalCards,
            long completedSessions,
            long totalReviews,
            int accuracy,
            int currentStreak,
            long weeklyStudyMinutes
    ) {
        return List.of(
                new AchievementResponse(
                        "FIRST_SET",
                        "First Set",
                        "trophy",
                        totalSets >= 1,
                        totalSets,
                        1
                ),
                new AchievementResponse(
                        "WEEK_STREAK",
                        "Week Streak",
                        "flame",
                        currentStreak >= 7,
                        currentStreak,
                        7
                ),
                new AchievementResponse(
                        "HUNDRED_CARDS",
                        "100 Cards",
                        "brain",
                        totalCards >= 100,
                        totalCards,
                        100
                ),
                new AchievementResponse(
                        "SPEED_RUN",
                        "Speed Run",
                        "flash",
                        weeklyStudyMinutes >= 60,
                        weeklyStudyMinutes,
                        60
                ),
                new AchievementResponse(
                        "PERFECT",
                        "Perfect",
                        "ribbon",
                        totalReviews >= 10 &&
                                accuracy == 100,
                        accuracy,
                        100
                ),
                new AchievementResponse(
                        "SHARPSHOOTER",
                        "Sharpshot",
                        "target",
                        totalReviews >= 25 &&
                                accuracy >= 90,
                        accuracy,
                        90
                ),
                new AchievementResponse(
                        "TOP_LEARNER",
                        "Top Learner",
                        "star",
                        completedSessions >= 25,
                        completedSessions,
                        25
                ),
                new AchievementResponse(
                        "COMMITTED",
                        "Committed",
                        "heart",
                        totalReviews >= 500,
                        totalReviews,
                        500
                )
        );
    }
}