package de.fliplearn.backend.service;

import de.fliplearn.backend.dto.CreateFlashcardRequest;
import de.fliplearn.backend.dto.FlashcardResponse;
import de.fliplearn.backend.dto.UpdateFlashcardRequest;
import de.fliplearn.backend.entity.Flashcard;
import de.fliplearn.backend.entity.FlashcardSet;
import de.fliplearn.backend.exception.ResourceNotFoundException;
import de.fliplearn.backend.repository.FlashcardRepository;
import de.fliplearn.backend.repository.FlashcardSetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FlashcardService {

    private final FlashcardRepository flashcardRepository;
    private final FlashcardSetRepository flashcardSetRepository;

    public FlashcardService(
            FlashcardRepository flashcardRepository,
            FlashcardSetRepository flashcardSetRepository
    ) {
        this.flashcardRepository = flashcardRepository;
        this.flashcardSetRepository = flashcardSetRepository;
    }

    @Transactional(readOnly = true)
    public List<FlashcardResponse> getCards(
            Long setId,
            String email
    ) {
        findOwnedSet(setId, email);

        return flashcardRepository
                .findAllByFlashcardSetIdOrderByCreatedAtAsc(setId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FlashcardResponse getCard(
            Long setId,
            Long cardId,
            String email
    ) {
        return toResponse(
                findOwnedCard(setId, cardId, email)
        );
    }

    @Transactional
    public FlashcardResponse createCard(
            Long setId,
            CreateFlashcardRequest request,
            String email
    ) {
        FlashcardSet set = findOwnedSet(setId, email);

        Flashcard card = new Flashcard(
                set,
                request.front().trim(),
                request.back().trim()
        );

        Flashcard savedCard =
                flashcardRepository.save(card);

        return toResponse(savedCard);
    }

    @Transactional
    public FlashcardResponse updateCard(
            Long setId,
            Long cardId,
            UpdateFlashcardRequest request,
            String email
    ) {
        Flashcard card =
                findOwnedCard(setId, cardId, email);

        card.setFront(request.front().trim());
        card.setBack(request.back().trim());
        card.setFavorite(request.favorite());

        Flashcard savedCard =
                flashcardRepository.save(card);

        return toResponse(savedCard);
    }

    @Transactional
    public void deleteCard(
            Long setId,
            Long cardId,
            String email
    ) {
        Flashcard card =
                findOwnedCard(setId, cardId, email);

        flashcardRepository.delete(card);
    }

    private FlashcardSet findOwnedSet(
            Long setId,
            String email
    ) {
        return flashcardSetRepository
                .findByIdAndOwnerEmailIgnoreCase(
                        setId,
                        email
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Lernset wurde nicht gefunden."
                        )
                );
    }

    private Flashcard findOwnedCard(
            Long setId,
            Long cardId,
            String email
    ) {
        return flashcardRepository
                .findByIdAndFlashcardSetIdAndFlashcardSetOwnerEmailIgnoreCase(
                        cardId,
                        setId,
                        email
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Lernkarte wurde nicht gefunden."
                        )
                );
    }

    private FlashcardResponse toResponse(
            Flashcard card
    ) {
        return new FlashcardResponse(
                card.getId(),
                card.getFlashcardSet().getId(),
                card.getFront(),
                card.getBack(),
                card.isFavorite(),
                card.getCreatedAt(),
                card.getUpdatedAt()
        );
    }
}