package de.fliplearn.backend.study.repository;

import de.fliplearn.backend.study.entity.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;

import java.util.List;
import java.util.Optional;

public interface StudySessionRepository
        extends JpaRepository<StudySession, Long> {

    Optional<StudySession>
    findByIdAndUserEmailIgnoreCase(
            Long sessionId,
            String email
    );

    List<StudySession>
    findAllByUserEmailIgnoreCaseOrderByStartedAtDesc(
            String email
    );

    boolean existsByIdAndUserEmailIgnoreCase(
            Long sessionId,
            String email
    );

    long countByUserEmailIgnoreCaseAndCompletedAtIsNotNull(
            String email
    );

    @Query(
            value = """
                SELECT COALESCE(
                    SUM(
                        EXTRACT(
                            EPOCH FROM
                            (ss.completed_at - ss.started_at)
                        )
                    ),
                    0
                )
                FROM study_sessions ss
                INNER JOIN app_users u
                    ON u.id = ss.user_id
                WHERE LOWER(u.email) = LOWER(:email)
                  AND ss.completed_at IS NOT NULL
                  AND ss.started_at >= :start
                  AND ss.started_at < :end
                """,
            nativeQuery = true
    )
    double sumCompletedStudySeconds(
            @Param("email") String email,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end
    );
}