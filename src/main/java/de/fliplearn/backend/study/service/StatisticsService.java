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
                        "First Set",
                        "Erstelle dein erstes Lernset.",
                        "trophy",
                        totalSets,
                        1
                ),
                createAchievement(
                        "SET_COLLECTOR_5",
                        "Set Collector",
                        "Erstelle fünf Lernsets.",
                        "layers",
                        totalSets,
                        5
                ),
                createAchievement(
                        "SET_COLLECTOR_10",
                        "Organized Learner",
                        "Erstelle zehn Lernsets.",
                        "layers",
                        totalSets,
                        10
                ),
                createAchievement(
                        "SET_COLLECTOR_25",
                        "Knowledge Library",
                        "Erstelle 25 Lernsets.",
                        "library",
                        totalSets,
                        25
                ),
                createAchievement(
                        "SET_COLLECTOR_50",
                        "Master Librarian",
                        "Erstelle 50 Lernsets.",
                        "library",
                        totalSets,
                        50
                ),

                /*
                 * Erstellte Lernkarten
                 */
                createAchievement(
                        "FIRST_CARD",
                        "First Card",
                        "Erstelle deine erste Lernkarte.",
                        "card",
                        totalCards,
                        1
                ),
                createAchievement(
                        "TEN_CARDS",
                        "Getting Started",
                        "Erstelle zehn Lernkarten.",
                        "card",
                        totalCards,
                        10
                ),
                createAchievement(
                        "TWENTY_FIVE_CARDS",
                        "Card Builder",
                        "Erstelle 25 Lernkarten.",
                        "card",
                        totalCards,
                        25
                ),
                createAchievement(
                        "FIFTY_CARDS",
                        "Deck Architect",
                        "Erstelle 50 Lernkarten.",
                        "brain",
                        totalCards,
                        50
                ),
                createAchievement(
                        "HUNDRED_CARDS",
                        "100 Cards",
                        "Erstelle insgesamt 100 Lernkarten.",
                        "brain",
                        totalCards,
                        100
                ),
                createAchievement(
                        "TWO_HUNDRED_FIFTY_CARDS",
                        "Knowledge Crafter",
                        "Erstelle 250 Lernkarten.",
                        "brain",
                        totalCards,
                        250
                ),
                createAchievement(
                        "FIVE_HUNDRED_CARDS",
                        "Card Factory",
                        "Erstelle 500 Lernkarten.",
                        "flash",
                        totalCards,
                        500
                ),
                createAchievement(
                        "THOUSAND_CARDS",
                        "Encyclopedia",
                        "Erstelle 1.000 Lernkarten.",
                        "library",
                        totalCards,
                        1000
                ),

                /*
                 * Abgeschlossene Lernsitzungen
                 */
                createAchievement(
                        "FIRST_SESSION",
                        "First Session",
                        "Schließe deine erste Lernsitzung ab.",
                        "play",
                        completedSessions,
                        1
                ),
                createAchievement(
                        "FIVE_SESSIONS",
                        "Study Habit",
                        "Schließe fünf Lernsitzungen ab.",
                        "school",
                        completedSessions,
                        5
                ),
                createAchievement(
                        "TEN_SESSIONS",
                        "Dedicated Student",
                        "Schließe zehn Lernsitzungen ab.",
                        "school",
                        completedSessions,
                        10
                ),
                createAchievement(
                        "TWENTY_FIVE_SESSIONS",
                        "Top Learner",
                        "Schließe 25 Lernsitzungen ab.",
                        "star",
                        completedSessions,
                        25
                ),
                createAchievement(
                        "FIFTY_SESSIONS",
                        "Study Veteran",
                        "Schließe 50 Lernsitzungen ab.",
                        "medal",
                        completedSessions,
                        50
                ),
                createAchievement(
                        "HUNDRED_SESSIONS",
                        "Session Master",
                        "Schließe 100 Lernsitzungen ab.",
                        "trophy",
                        completedSessions,
                        100
                ),
                createAchievement(
                        "TWO_HUNDRED_FIFTY_SESSIONS",
                        "Learning Machine",
                        "Schließe 250 Lernsitzungen ab.",
                        "flash",
                        completedSessions,
                        250
                ),
                createAchievement(
                        "FIVE_HUNDRED_SESSIONS",
                        "Unstoppable",
                        "Schließe 500 Lernsitzungen ab.",
                        "rocket",
                        completedSessions,
                        500
                ),

                /*
                 * Bearbeitete Karten / Reviews
                 */
                createAchievement(
                        "FIRST_REVIEW",
                        "First Answer",
                        "Bewerte deine erste Lernkarte.",
                        "check",
                        totalReviews,
                        1
                ),
                createAchievement(
                        "TEN_REVIEWS",
                        "Warm Up",
                        "Bearbeite zehn Lernkarten.",
                        "check",
                        totalReviews,
                        10
                ),
                createAchievement(
                        "FIFTY_REVIEWS",
                        "Quick Learner",
                        "Bearbeite 50 Lernkarten.",
                        "flash",
                        totalReviews,
                        50
                ),
                createAchievement(
                        "HUNDRED_REVIEWS",
                        "Century",
                        "Bearbeite 100 Lernkarten.",
                        "medal",
                        totalReviews,
                        100
                ),
                createAchievement(
                        "TWO_HUNDRED_FIFTY_REVIEWS",
                        "Focused Mind",
                        "Bearbeite 250 Lernkarten.",
                        "brain",
                        totalReviews,
                        250
                ),
                createAchievement(
                        "FIVE_HUNDRED_REVIEWS",
                        "Committed",
                        "Bearbeite insgesamt 500 Lernkarten.",
                        "heart",
                        totalReviews,
                        500
                ),
                createAchievement(
                        "THOUSAND_REVIEWS",
                        "Review Master",
                        "Bearbeite 1.000 Lernkarten.",
                        "trophy",
                        totalReviews,
                        1000
                ),
                createAchievement(
                        "TWO_THOUSAND_FIVE_HUNDRED_REVIEWS",
                        "Knowledge Seeker",
                        "Bearbeite 2.500 Lernkarten.",
                        "search",
                        totalReviews,
                        2500
                ),
                createAchievement(
                        "FIVE_THOUSAND_REVIEWS",
                        "Memory Athlete",
                        "Bearbeite 5.000 Lernkarten.",
                        "fitness",
                        totalReviews,
                        5000
                ),
                createAchievement(
                        "TEN_THOUSAND_REVIEWS",
                        "Legendary Learner",
                        "Bearbeite 10.000 Lernkarten.",
                        "diamond",
                        totalReviews,
                        10000
                ),

                /*
                 * Lernserien
                 */
                createAchievement(
                        "STREAK_2",
                        "Back Again",
                        "Lerne an zwei Tagen hintereinander.",
                        "flame",
                        currentStreak,
                        2
                ),
                createAchievement(
                        "STREAK_3",
                        "Three Day Spark",
                        "Lerne an drei Tagen hintereinander.",
                        "flame",
                        currentStreak,
                        3
                ),
                createAchievement(
                        "WEEK_STREAK",
                        "Week Streak",
                        "Lerne an sieben Tagen hintereinander.",
                        "flame",
                        currentStreak,
                        7
                ),
                createAchievement(
                        "STREAK_14",
                        "Two Week Streak",
                        "Lerne an 14 Tagen hintereinander.",
                        "flame",
                        currentStreak,
                        14
                ),
                createAchievement(
                        "STREAK_30",
                        "Monthly Momentum",
                        "Lerne an 30 Tagen hintereinander.",
                        "calendar",
                        currentStreak,
                        30
                ),
                createAchievement(
                        "STREAK_60",
                        "Habit Formed",
                        "Lerne an 60 Tagen hintereinander.",
                        "calendar",
                        currentStreak,
                        60
                ),
                createAchievement(
                        "STREAK_100",
                        "Century Streak",
                        "Lerne an 100 Tagen hintereinander.",
                        "trophy",
                        currentStreak,
                        100
                ),
                createAchievement(
                        "STREAK_180",
                        "Half Year Hero",
                        "Lerne an 180 Tagen hintereinander.",
                        "medal",
                        currentStreak,
                        180
                ),
                createAchievement(
                        "STREAK_365",
                        "Year of Learning",
                        "Lerne ein ganzes Jahr ohne Unterbrechung.",
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
                        "On the Right Track",
                        "Erreiche nach mindestens zehn Bewertungen 60 % Genauigkeit.",
                        "target",
                        totalReviews >= 10 && accuracy >= 60,
                        accuracy,
                        60
                ),
                createConditionalAchievement(
                        "ACCURACY_70",
                        "Steady Aim",
                        "Erreiche nach mindestens 25 Bewertungen 70 % Genauigkeit.",
                        "target",
                        totalReviews >= 25 && accuracy >= 70,
                        accuracy,
                        70
                ),
                createConditionalAchievement(
                        "ACCURACY_80",
                        "Accurate Learner",
                        "Erreiche nach mindestens 50 Bewertungen 80 % Genauigkeit.",
                        "target",
                        totalReviews >= 50 && accuracy >= 80,
                        accuracy,
                        80
                ),
                createConditionalAchievement(
                        "ACCURACY_90",
                        "Sharpshooter",
                        "Erreiche nach mindestens 100 Bewertungen 90 % Genauigkeit.",
                        "target",
                        totalReviews >= 100 && accuracy >= 90,
                        accuracy,
                        90
                ),
                createConditionalAchievement(
                        "ACCURACY_95",
                        "Precision Expert",
                        "Erreiche nach mindestens 250 Bewertungen 95 % Genauigkeit.",
                        "radio",
                        totalReviews >= 250 && accuracy >= 95,
                        accuracy,
                        95
                ),
                createConditionalAchievement(
                        "PERFECT",
                        "Perfect",
                        "Erreiche nach mindestens 100 Bewertungen 100 % Genauigkeit.",
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
                        "Quarter Hour",
                        "Lerne mindestens 15 Minuten in einer Woche.",
                        "time",
                        weeklyStudyMinutes,
                        15
                ),
                createAchievement(
                        "STUDY_TIME_30",
                        "Focused Half Hour",
                        "Lerne mindestens 30 Minuten in einer Woche.",
                        "time",
                        weeklyStudyMinutes,
                        30
                ),
                createAchievement(
                        "SPEED_RUN",
                        "Speed Run",
                        "Sammle mindestens 60 Lernminuten in einer Woche.",
                        "flash",
                        weeklyStudyMinutes,
                        60
                ),
                createAchievement(
                        "STUDY_TIME_120",
                        "Deep Focus",
                        "Lerne mindestens zwei Stunden in einer Woche.",
                        "time",
                        weeklyStudyMinutes,
                        120
                ),
                createAchievement(
                        "STUDY_TIME_300",
                        "Five Hour Week",
                        "Lerne mindestens fünf Stunden in einer Woche.",
                        "time",
                        weeklyStudyMinutes,
                        300
                ),
                createAchievement(
                        "STUDY_TIME_600",
                        "Study Marathon",
                        "Lerne mindestens zehn Stunden in einer Woche.",
                        "fitness",
                        weeklyStudyMinutes,
                        600
                )
        );
    }

    private AchievementResponse createAchievement(
            String code,
            String title,
            String description,
            String icon,
            long currentValue,
            long targetValue
    ) {
        boolean earned =
                currentValue >= targetValue;

        return new AchievementResponse(
                code,
                title,
                description,
                icon,
                earned,
                currentValue,
                targetValue,
                calculateProgress(
                        currentValue,
                        targetValue
                )
        );
    }

    private AchievementResponse createConditionalAchievement(
            String code,
            String title,
            String description,
            String icon,
            boolean earned,
            long currentValue,
            long targetValue
    ) {
        return new AchievementResponse(
                code,
                title,
                description,
                icon,
                earned,
                currentValue,
                targetValue,
                earned
                        ? 100
                        : calculateProgress(
                        currentValue,
                        targetValue
                )
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