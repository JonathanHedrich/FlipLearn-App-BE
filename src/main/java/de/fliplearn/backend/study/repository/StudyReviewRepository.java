package de.fliplearn.backend.study.repository;

import de.fliplearn.backend.statistics.repository.DailyStudyActivityProjection;
import de.fliplearn.backend.study.entity.StudyReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import de.fliplearn.backend.statistics.repository.SetAccuracyProjection;

public interface StudyReviewRepository
        extends JpaRepository<StudyReview, Long> {

    List<StudyReview>
    findAllBySessionIdOrderByReviewedAtAsc(
            Long sessionId
    );

    boolean existsBySessionIdAndCardId(
            Long sessionId,
            Long cardId
    );

    long countBySessionUserEmailIgnoreCase(
            String email
    );

    long countBySessionUserEmailIgnoreCaseAndAnsweredCorrectlyTrue(
            String email
    );

    long countBySessionUserEmailIgnoreCaseAndReviewedAtBetween(
            String email,
            OffsetDateTime start,
            OffsetDateTime end
    );

    long countBySessionUserEmailIgnoreCaseAndAnsweredCorrectlyTrueAndReviewedAtBetween(
            String email,
            OffsetDateTime start,
            OffsetDateTime end
    );

    @Query(
            value = """
                    SELECT
                        CAST(sr.reviewed_at AS DATE) AS activityDate,
                        COUNT(sr.id) AS reviewCount,
                        SUM(
                            CASE
                                WHEN sr.answered_correctly = TRUE
                                THEN 1
                                ELSE 0
                            END
                        ) AS correctCount
                    FROM study_reviews sr
                    INNER JOIN study_sessions ss
                        ON ss.id = sr.session_id
                    INNER JOIN app_users u
                        ON u.id = ss.user_id
                    WHERE LOWER(u.email) = LOWER(:email)
                      AND sr.reviewed_at >= :start
                      AND sr.reviewed_at < :end
                    GROUP BY CAST(sr.reviewed_at AS DATE)
                    ORDER BY CAST(sr.reviewed_at AS DATE)
                    """,
            nativeQuery = true
    )
    List<DailyStudyActivityProjection> findDailyActivity(
            @Param("email") String email,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end
    );

    @Query(
            value = """
                SELECT
                    fs.id AS setId,
                    fs.title AS title,
                    fs.color AS color,
                    COUNT(sr.id) AS totalReviews,
                    SUM(
                        CASE
                            WHEN sr.answered_correctly = TRUE
                            THEN 1
                            ELSE 0
                        END
                    ) AS correctReviews
                FROM flashcard_sets fs
                LEFT JOIN flashcards fc
                    ON fc.set_id = fs.id
                LEFT JOIN study_reviews sr
                    ON sr.card_id = fc.id
                INNER JOIN app_users u
                    ON u.id = fs.owner_id
                WHERE LOWER(u.email) = LOWER(:email)
                GROUP BY
                    fs.id,
                    fs.title,
                    fs.color
                ORDER BY fs.updated_at DESC
                """,
            nativeQuery = true
    )
    List<SetAccuracyProjection> findSetAccuracies(
            @Param("email") String email
    );
}