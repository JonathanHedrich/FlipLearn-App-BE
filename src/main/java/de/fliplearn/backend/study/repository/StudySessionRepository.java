package de.fliplearn.backend.study.repository;

import de.fliplearn.backend.study.entity.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;

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
}