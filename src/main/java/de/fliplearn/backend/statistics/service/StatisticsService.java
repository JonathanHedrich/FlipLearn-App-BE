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

                /*
                 * Lernsets
                 */
                createAchievement(
                        "FIRST_SET",
                        "trophy",
                        totalSets,
                        1
                ),
                createAchievement(
                        "SET_COLLECTOR_5",
                        "layers",
                        totalSets,
                        5
                ),
                createAchievement(
                        "SET_COLLECTOR_10",
                        "layers",
                        totalSets,
                        10
                ),
                createAchievement(
                        "SET_COLLECTOR_25",
                        "library",
                        totalSets,
                        25
                ),
                createAchievement(
                        "SET_COLLECTOR_50",
                        "library",
                        totalSets,
                        50
                ),

                /*
                 * Erstellte Lernkarten
                 */
                createAchievement(
                        "FIRST_CARD",
                        "card",
                        totalCards,
                        1
                ),
                createAchievement(
                        "TEN_CARDS",
                        "card",
                        totalCards,
                        10
                ),
                createAchievement(
                        "TWENTY_FIVE_CARDS",
                        "card",
                        totalCards,
                        25
                ),
                createAchievement(
                        "FIFTY_CARDS",
                        "brain",
                        totalCards,
                        50
                ),
                createAchievement(
                        "HUNDRED_CARDS",
                        "brain",
                        totalCards,
                        100
                ),
                createAchievement(
                        "TWO_HUNDRED_FIFTY_CARDS",
                        "brain",
                        totalCards,
                        250
                ),
                createAchievement(
                        "FIVE_HUNDRED_CARDS",
                        "flash",
                        totalCards,
                        500
                ),
                createAchievement(
                        "THOUSAND_CARDS",
                        "library",
                        totalCards,
                        1000
                ),

                /*
                 * Abgeschlossene Lernsitzungen
                 */
                createAchievement(
                        "FIRST_SESSION",
                        "play",
                        completedSessions,
                        1
                ),
                createAchievement(
                        "FIVE_SESSIONS",
                        "school",
                        completedSessions,
                        5
                ),
                createAchievement(
                        "TEN_SESSIONS",
                        "school",
                        completedSessions,
                        10
                ),
                createAchievement(
                        "TWENTY_FIVE_SESSIONS",
                        "star",
                        completedSessions,
                        25
                ),
                createAchievement(
                        "FIFTY_SESSIONS",
                        "medal",
                        completedSessions,
                        50
                ),
                createAchievement(
                        "HUNDRED_SESSIONS",
                        "trophy",
                        completedSessions,
                        100
                ),
                createAchievement(
                        "TWO_HUNDRED_FIFTY_SESSIONS",
                        "flash",
                        completedSessions,
                        250
                ),
                createAchievement(
                        "FIVE_HUNDRED_SESSIONS",
                        "rocket",
                        completedSessions,
                        500
                ),

                /*
                 * Bearbeitete Karten / Reviews
                 */
                createAchievement(
                        "FIRST_REVIEW",
                        "check",
                        totalReviews,
                        1
                ),
                createAchievement(
                        "TEN_REVIEWS",
                        "check",
                        totalReviews,
                        10
                ),
                createAchievement(
                        "FIFTY_REVIEWS",
                        "flash",
                        totalReviews,
                        50
                ),
                createAchievement(
                        "HUNDRED_REVIEWS",
                        "medal",
                        totalReviews,
                        100
                ),
                createAchievement(
                        "TWO_HUNDRED_FIFTY_REVIEWS",
                        "brain",
                        totalReviews,
                        250
                ),
                createAchievement(
                        "FIVE_HUNDRED_REVIEWS",
                        "heart",
                        totalReviews,
                        500
                ),
                createAchievement(
                        "THOUSAND_REVIEWS",
                        "trophy",
                        totalReviews,
                        1000
                ),
                createAchievement(
                        "TWO_THOUSAND_FIVE_HUNDRED_REVIEWS",
                        "search",
                        totalReviews,
                        2500
                ),
                createAchievement(
                        "FIVE_THOUSAND_REVIEWS",
                        "fitness",
                        totalReviews,
                        5000
                ),
                createAchievement(
                        "TEN_THOUSAND_REVIEWS",
                        "diamond",
                        totalReviews,
                        10000
                ),

                /*
                 * Lernserien
                 */
                createAchievement(
                        "STREAK_2",
                        "flame",
                        currentStreak,
                        2
                ),
                createAchievement(
                        "STREAK_3",
                        "flame",
                        currentStreak,
                        3
                ),
                createAchievement(
                        "WEEK_STREAK",
                        "flame",
                        currentStreak,
                        7
                ),
                createAchievement(
                        "STREAK_14",
                        "flame",
                        currentStreak,
                        14
                ),
                createAchievement(
                        "STREAK_30",
                        "calendar",
                        currentStreak,
                        30
                ),
                createAchievement(
                        "STREAK_60",
                        "calendar",
                        currentStreak,
                        60
                ),
                createAchievement(
                        "STREAK_100",
                        "trophy",
                        currentStreak,
                        100
                ),
                createAchievement(
                        "STREAK_180",
                        "medal",
                        currentStreak,
                        180
                ),
                createAchievement(
                        "STREAK_365",
                        "diamond",
                        currentStreak,
                        365
                ),

                /*
                 * Genauigkeit
                 *
                 * Diese Achievements werden erst nach einer
                 * Mindestzahl von Reviews freigeschaltet.
                 */
                createConditionalAchievement(
                        "ACCURACY_60",
                        "target",
                        totalReviews >= 10 && accuracy >= 60,
                        accuracy,
                        60
                ),
                createConditionalAchievement(
                        "ACCURACY_70",
                        "target",
                        totalReviews >= 25 && accuracy >= 70,
                        accuracy,
                        70
                ),
                createConditionalAchievement(
                        "ACCURACY_80",
                        "target",
                        totalReviews >= 50 && accuracy >= 80,
                        accuracy,
                        80
                ),
                createConditionalAchievement(
                        "ACCURACY_90",
                        "target",
                        totalReviews >= 100 && accuracy >= 90,
                        accuracy,
                        90
                ),
                createConditionalAchievement(
                        "ACCURACY_95",
                        "radio",
                        totalReviews >= 250 && accuracy >= 95,
                        accuracy,
                        95
                ),
                createConditionalAchievement(
                        "PERFECT",
                        "ribbon",
                        totalReviews >= 100 && accuracy == 100,
                        accuracy,
                        100
                ),

                /*
                 * Wöchentliche Lernzeit
                 */
                createAchievement(
                        "STUDY_TIME_15",
                        "time",
                        weeklyStudyMinutes,
                        15
                ),
                createAchievement(
                        "STUDY_TIME_30",
                        "time",
                        weeklyStudyMinutes,
                        30
                ),
                createAchievement(
                        "SPEED_RUN",
                        "flash",
                        weeklyStudyMinutes,
                        60
                ),
                createAchievement(
                        "STUDY_TIME_120",
                        "time",
                        weeklyStudyMinutes,
                        120
                ),
                createAchievement(
                        "STUDY_TIME_300",
                        "time",
                        weeklyStudyMinutes,
                        300
                ),
                createAchievement(
                        "STUDY_TIME_600",
                        "fitness",
                        weeklyStudyMinutes,
                        600
                )
        );
    }

    private AchievementResponse createAchievement(
            String code,
            String icon,
            long currentValue,
            long targetValue
    ) {
        boolean earned = currentValue >= targetValue;

        return new AchievementResponse(
                code,
                icon,
                earned,
                currentValue,
                targetValue,
                calculateProgress(currentValue, targetValue),
                Map.of()
        );
    }

    private AchievementResponse createConditionalAchievement(
            String code,
            String icon,
            boolean earned,
            long currentValue,
            long targetValue
    ) {
        return new AchievementResponse(
                code,
                icon,
                earned,
                currentValue,
                targetValue,
                earned
                        ? 100
                        : calculateProgress(currentValue, targetValue),
                Map.of()
        );
    }

    private int calculateProgress(
            long currentValue,
            long targetValue
    ) {
        if (targetValue <= 0) {
            return 0;
        }

        return Math.min(
                100,
                Math.max(
                        0,
                        Math.round(
                                currentValue
                                        * 100.0f
                                        / targetValue
                        )
                )
        );
    }
}