package de.fliplearn.backend.study.service;

import de.fliplearn.backend.entity.AppUser;
import de.fliplearn.backend.entity.Flashcard;
import de.fliplearn.backend.entity.FlashcardSet;
import de.fliplearn.backend.exception.ResourceConflictException;
import de.fliplearn.backend.exception.ResourceNotFoundException;
import de.fliplearn.backend.repository.AppUserRepository;
import de.fliplearn.backend.repository.FlashcardRepository;
import de.fliplearn.backend.repository.FlashcardSetRepository;
import de.fliplearn.backend.study.dto.StartStudySessionRequest;
import de.fliplearn.backend.study.dto.StudyCardResponse;
import de.fliplearn.backend.study.dto.StudyReviewResponse;
import de.fliplearn.backend.study.dto.StudySessionResponse;
import de.fliplearn.backend.study.dto.SubmitReviewRequest;
import de.fliplearn.backend.study.entity.StudyReview;
import de.fliplearn.backend.study.entity.StudySession;
import de.fliplearn.backend.study.repository.StudyReviewRepository;
import de.fliplearn.backend.study.repository.StudySessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.fliplearn.backend.study.entity.StudyMode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudyService {

    private final AppUserRepository appUserRepository;
    private final FlashcardSetRepository flashcardSetRepository;
    private final FlashcardRepository flashcardRepository;
    private final StudySessionRepository studySessionRepository;
    private final StudyReviewRepository studyReviewRepository;
    private final SpacedRepetitionService spacedRepetitionService;

    public StudyService(
            AppUserRepository appUserRepository,
            FlashcardSetRepository flashcardSetRepository,
            FlashcardRepository flashcardRepository,
            StudySessionRepository studySessionRepository,
            StudyReviewRepository studyReviewRepository,
            SpacedRepetitionService spacedRepetitionService
    ) {
        this.appUserRepository = appUserRepository;
        this.flashcardSetRepository = flashcardSetRepository;
        this.flashcardRepository = flashcardRepository;
        this.studySessionRepository = studySessionRepository;
        this.studyReviewRepository = studyReviewRepository;
        this.spacedRepetitionService = spacedRepetitionService;
    }

    @Transactional
    public StudySessionResponse startSession(
            StartStudySessionRequest request,
            String email
    ) {
        AppUser user = appUserRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Benutzer wurde nicht gefunden."
                        )
                );

        FlashcardSet set = flashcardSetRepository
                .findByIdAndOwnerEmailIgnoreCase(
                        request.setId(),
                        email
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Lernset wurde nicht gefunden."
                        )
                );

        List<Flashcard> cards = findStudyCards(
                set.getId(),
                request.mode()
        );

        if (cards.isEmpty()) {
            throw new ResourceConflictException(
                    request.mode() == StudyMode.DUE
                            ? "Für dieses Lernset sind aktuell keine Karten fällig."
                            : "Dieses Lernset enthält noch keine Karten."
            );
        }

        StudySession session = new StudySession(
                user,
                set,
                cards.size()
        );

        StudySession savedSession =
                studySessionRepository.save(session);

        return toSessionResponse(
                savedSession,
                cards
        );
    }

    private List<Flashcard> findStudyCards(
            Long setId,
            StudyMode mode
    ) {
        if (mode == StudyMode.ALL) {
            return flashcardRepository
                    .findAllByFlashcardSetIdOrderByCreatedAtAsc(
                            setId
                    );
        }

        return findDueCards(setId);
    }

    @Transactional
    public StudyReviewResponse submitReview(
            Long sessionId,
            SubmitReviewRequest request,
            String email
    ) {
        StudySession session = studySessionRepository
                .findByIdAndUserEmailIgnoreCase(
                        sessionId,
                        email
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Lernsitzung wurde nicht gefunden."
                        )
                );

        if (session.isComplete()) {
            throw new ResourceConflictException(
                    "Diese Lernsitzung wurde bereits abgeschlossen."
            );
        }

        Flashcard card = flashcardRepository
                .findByIdAndFlashcardSetIdAndFlashcardSetOwnerEmailIgnoreCase(
                        request.cardId(),
                        session.getFlashcardSet().getId(),
                        email
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Lernkarte wurde nicht gefunden."
                        )
                );

        boolean alreadyReviewed =
                studyReviewRepository
                        .existsBySessionIdAndCardId(
                                sessionId,
                                card.getId()
                        );

        if (alreadyReviewed) {
            throw new ResourceConflictException(
                    "Diese Karte wurde in der Sitzung bereits bewertet."
            );
        }

        OffsetDateTime reviewedAt =
                OffsetDateTime.now();

        int previousIntervalDays =
                card.getIntervalDays();

        BigDecimal previousEaseFactor =
                card.getEaseFactor();

        SchedulingResult schedulingResult =
                spacedRepetitionService.calculate(
                        card,
                        request.rating(),
                        reviewedAt
                );

        card.updateLearningState(
                schedulingResult.easeFactor(),
                schedulingResult.intervalDays(),
                schedulingResult.repetitions(),
                schedulingResult.nextReviewAt(),
                reviewedAt,
                schedulingResult.answeredCorrectly()
        );

        session.recordAnswer(
                schedulingResult.answeredCorrectly()
        );

        StudyReview review = new StudyReview(
                session,
                card,
                request.rating(),
                schedulingResult.answeredCorrectly(),
                previousIntervalDays,
                schedulingResult.intervalDays(),
                previousEaseFactor,
                schedulingResult.easeFactor()
        );

        StudyReview savedReview =
                studyReviewRepository.save(review);

        flashcardRepository.save(card);

        int setProgress = calculateSessionProgress(session);

        session.getFlashcardSet()
                .setProgress(setProgress);

        if (
                session.getAnsweredCards()
                        >= session.getTotalCards()
        ) {
            session.complete();
        }

        studySessionRepository.save(session);

        return new StudyReviewResponse(
                savedReview.getId(),
                session.getId(),
                card.getId(),
                request.rating(),
                schedulingResult.answeredCorrectly(),
                previousIntervalDays,
                schedulingResult.intervalDays(),
                previousEaseFactor,
                schedulingResult.easeFactor(),
                schedulingResult.nextReviewAt(),
                session.isComplete(),
                session.getCorrectAnswers(),
                session.getIncorrectAnswers(),
                setProgress
        );
    }

    @Transactional(readOnly = true)
    public StudySessionResponse getSession(
            Long sessionId,
            String email
    ) {
        StudySession session = studySessionRepository
                .findByIdAndUserEmailIgnoreCase(
                        sessionId,
                        email
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Lernsitzung wurde nicht gefunden."
                        )
                );

        List<Flashcard> cards =
                session.getReviews()
                        .stream()
                        .map(StudyReview::getCard)
                        .toList();

        return toSessionResponse(
                session,
                cards
        );
    }

    private List<Flashcard> findDueCards(
            Long setId
    ) {
        OffsetDateTime now = OffsetDateTime.now();

        List<Flashcard> newCards =
                flashcardRepository
                        .findAllByFlashcardSetIdAndNextReviewAtIsNullOrderByCreatedAtAsc(
                                setId
                        );

        List<Flashcard> dueCards =
                flashcardRepository
                        .findAllByFlashcardSetIdAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc(
                                setId,
                                now
                        );

        Map<Long, Flashcard> uniqueCards =
                new LinkedHashMap<>();

        newCards.forEach(card ->
                uniqueCards.put(card.getId(), card)
        );

        dueCards.forEach(card ->
                uniqueCards.put(card.getId(), card)
        );

        return new ArrayList<>(
                uniqueCards.values()
        );
    }

    private StudySessionResponse toSessionResponse(
            StudySession session,
            List<Flashcard> cards
    ) {
        List<StudyCardResponse> cardResponses =
                cards.stream()
                        .map(this::toCardResponse)
                        .toList();

        return new StudySessionResponse(
                session.getId(),
                session.getFlashcardSet().getId(),
                session.getFlashcardSet().getTitle(),
                session.getStartedAt(),
                session.getTotalCards(),
                session.getCorrectAnswers(),
                session.getIncorrectAnswers(),
                session.isComplete(),
                cardResponses
        );
    }

    private StudyCardResponse toCardResponse(
            Flashcard card
    ) {
        return new StudyCardResponse(
                card.getId(),
                card.getFront(),
                card.getBack(),
                card.isFavorite(),
                card.getIntervalDays(),
                card.getRepetitions(),
                card.getNextReviewAt()
        );
    }

    private int calculateAccuracy(
            StudySession session
    ) {
        int answered = session.getAnsweredCards();

        if (answered == 0) {
            return 0;
        }

        return Math.round(
                (
                        session.getCorrectAnswers()
                                * 100.0f
                ) / answered
        );
    }

    private int calculateSessionProgress(
            StudySession session
    ) {
        if (session.getTotalCards() <= 0) {
            return 0;
        }

        return Math.min(
                100,
                Math.round(
                        session.getAnsweredCards()
                                * 100.0f
                                / session.getTotalCards()
                )
        );
    }
}