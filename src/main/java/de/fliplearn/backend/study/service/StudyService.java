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
import de.fliplearn.backend.study.repository.StudySessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.fliplearn.backend.study.entity.StudyMode;
import de.fliplearn.backend.study.repository.StudyReviewRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import java.util.LinkedHashMap;
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

        StudyMode mode = request.mode() != null
                ? request.mode()
                : StudyMode.ALL;

        List<Flashcard> cards = findStudyCards(
                set.getId(),
                mode,
                request.sourceSessionId(),
                email
        );

        if (cards.isEmpty()) {
            throw new ResourceConflictException(
                    getEmptyModeMessage(mode)
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
                cards,
                mode
        );
    }

    private List<Flashcard> findStudyCards(
            Long setId,
            StudyMode mode,
            Long sourceSessionId,
            String email
    ) {
        List<Flashcard> allCards =
                flashcardRepository
                        .findAllByFlashcardSetIdOrderByCreatedAtAsc(
                                setId
                        );

        return switch (mode) {
            case ALL,
                 MARATHON,
                 LIGHTNING,
                 EXAM ->
                    allCards;

            case RANDOM ->
                    shuffledCopy(allCards);

            case FAVORITES ->
                    allCards.stream()
                            .filter(Flashcard::isFavorite)
                            .toList();

            case DIFFICULT ->
                    findDifficultCards(
                            setId,
                            allCards
                    );

            case NEW_ONLY ->
                    allCards.stream()
                            .filter(card ->
                                    card.getTotalReviews() == 0
                            )
                            .toList();

            case DUE,
                 DUE_ONLY ->
                    findDueCards(setId);

            case FAVORITES_DUE ->
                    findDueCards(setId)
                            .stream()
                            .filter(Flashcard::isFavorite)
                            .toList();

            case WRONG_ONLY ->
                    findWrongCards(
                            setId,
                            sourceSessionId,
                            email
                    );
        };
    }

    private List<Flashcard> shuffledCopy(
            List<Flashcard> cards
    ) {
        List<Flashcard> shuffled =
                new ArrayList<>(cards);

        java.util.Collections.shuffle(
                shuffled
        );

        return shuffled;
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
                cards,
                StudyMode.ALL
        );
    }

    private List<Flashcard> findDueCards(
            Long setId
    ) {
        return flashcardRepository
                .findAllByFlashcardSetIdAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc(
                        setId,
                        OffsetDateTime.now()
                );
    }

    private int compareDifficulty(
            Flashcard first,
            Flashcard second
    ) {
        boolean firstIsNew =
                first.getTotalReviews() == 0;

        boolean secondIsNew =
                second.getTotalReviews() == 0;

        if (firstIsNew != secondIsNew) {
            return firstIsNew ? 1 : -1;
        }

        double firstAccuracy =
                first.getTotalReviews() == 0
                        ? 1.0
                        : (double) first.getCorrectReviews()
                        / first.getTotalReviews();

        double secondAccuracy =
                second.getTotalReviews() == 0
                        ? 1.0
                        : (double) second.getCorrectReviews()
                        / second.getTotalReviews();

        int accuracyComparison =
                Double.compare(
                        firstAccuracy,
                        secondAccuracy
                );

        if (accuracyComparison != 0) {
            return accuracyComparison;
        }

        int easeComparison =
                first.getEaseFactor()
                        .compareTo(
                                second.getEaseFactor()
                        );

        if (easeComparison != 0) {
            return easeComparison;
        }

        int intervalComparison =
                Integer.compare(
                        first.getIntervalDays(),
                        second.getIntervalDays()
                );

        if (intervalComparison != 0) {
            return intervalComparison;
        }

        return second.getUpdatedAt()
                .compareTo(first.getUpdatedAt());
    }

    private StudySessionResponse toSessionResponse(
            StudySession session,
            List<Flashcard> cards,
            StudyMode mode
    ) {
        List<StudyCardResponse> cardResponses =
                cards.stream()
                        .map(this::toCardResponse)
                        .toList();

        return new StudySessionResponse(
                session.getId(),
                session.getFlashcardSet().getId(),
                session.getFlashcardSet().getTitle(),
                mode,
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

    private String getEmptyModeMessage(
            StudyMode mode
    ) {
        return switch (mode) {
            case DUE, DUE_ONLY ->
                    "Für dieses Lernset sind aktuell keine Karten fällig.";

            case FAVORITES ->
                    "Dieses Lernset enthält keine favorisierten Karten.";

            case FAVORITES_DUE ->
                    "Es gibt aktuell keine favorisierten und gleichzeitig fälligen Karten.";

            case NEW_ONLY ->
                    "Dieses Lernset enthält keine neuen Karten.";

            case WRONG_ONLY ->
                    "Es gibt aktuell keine zuletzt falsch beantworteten Karten.";

            default ->
                    "Dieses Lernset enthält keine Karten.";
        };
    }

    private List<Flashcard> findWrongCards(
            Long setId,
            Long sourceSessionId,
            String email
    ) {
        if (sourceSessionId != null) {
            StudySession sourceSession =
                    studySessionRepository
                            .findByIdAndUserEmailIgnoreCase(
                                    sourceSessionId,
                                    email
                            )
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Die vorherige Marathon-Runde wurde nicht gefunden."
                                    )
                            );

            if (
                    !sourceSession
                            .getFlashcardSet()
                            .getId()
                            .equals(setId)
            ) {
                throw new ResourceConflictException(
                        "Die Marathon-Runde gehört zu einem anderen Lernset."
                );
            }

            return studyReviewRepository
                    .findIncorrectCardsBySessionId(
                            sourceSessionId
                    );
        }

        /*
         * Normales Wrong Answers Only außerhalb
         * eines Marathons.
         */
        return studyReviewRepository
                .findCardsWhoseLatestReviewWasIncorrect(
                        setId
                );
    }

    private List<Flashcard> findDifficultCards(
            Long setId,
            List<Flashcard> allCards
    ) {
        List<Flashcard> lastAnsweredIncorrectly =
                studyReviewRepository
                        .findCardsWhoseLatestReviewWasIncorrect(
                                setId
                        );

        Map<Long, Flashcard> orderedCards =
                new LinkedHashMap<>();

        /*
         * Zuletzt falsch beantwortete Karten
         * kommen immer zuerst.
         */
        lastAnsweredIncorrectly.stream()
                .sorted(this::compareDifficulty)
                .forEach(card ->
                        orderedCards.put(
                                card.getId(),
                                card
                        )
                );

        /*
         * Danach folgen alle übrigen Karten,
         * ebenfalls nach Schwierigkeit sortiert.
         */
        allCards.stream()
                .sorted(this::compareDifficulty)
                .forEach(card ->
                        orderedCards.putIfAbsent(
                                card.getId(),
                                card
                        )
                );

        return new ArrayList<>(
                orderedCards.values()
        );
    }
}