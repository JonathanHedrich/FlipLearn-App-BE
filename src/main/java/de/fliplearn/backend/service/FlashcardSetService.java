package de.fliplearn.backend.service;

import de.fliplearn.backend.dto.CreateFlashcardSetRequest;
import de.fliplearn.backend.dto.FlashcardSetResponse;
import de.fliplearn.backend.entity.AppUser;
import de.fliplearn.backend.entity.FlashcardSet;
import de.fliplearn.backend.exception.ResourceNotFoundException;
import de.fliplearn.backend.repository.AppUserRepository;
import de.fliplearn.backend.repository.FlashcardSetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FlashcardSetService {

    private final FlashcardSetRepository flashcardSetRepository;
    private final AppUserRepository appUserRepository;

    public FlashcardSetService(
            FlashcardSetRepository flashcardSetRepository,
            AppUserRepository appUserRepository
    ) {
        this.flashcardSetRepository = flashcardSetRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public List<FlashcardSetResponse> getAllSets(
            String email
    ) {
        return flashcardSetRepository
                .findAllByOwnerEmailIgnoreCaseOrderByUpdatedAtDesc(
                        email
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FlashcardSetResponse getSet(
            Long setId,
            String email
    ) {
        FlashcardSet set = findOwnedSet(
                setId,
                email
        );

        return toResponse(set);
    }

    @Transactional
    public FlashcardSetResponse createSet(
            CreateFlashcardSetRequest request,
            String email
    ) {
        AppUser owner = appUserRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Benutzer wurde nicht gefunden."
                        )
                );

        FlashcardSet set = new FlashcardSet(
                owner,
                request.title().trim(),
                normalizeNullable(request.description()),
                normalizeNullable(request.folder()),
                request.color()
        );

        FlashcardSet savedSet =
                flashcardSetRepository.save(set);

        return toResponse(savedSet);
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

    private FlashcardSetResponse toResponse(
            FlashcardSet set
    ) {
        return new FlashcardSetResponse(
                set.getId(),
                set.getTitle(),
                set.getDescription(),
                set.getFolder(),
                set.getColor(),
                set.isFavorite(),
                set.getProgress(),
                set.getCards().size(),
                set.getCreatedAt(),
                set.getUpdatedAt()
        );
    }

    private String normalizeNullable(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}