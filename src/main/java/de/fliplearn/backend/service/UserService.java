package de.fliplearn.backend.service;

import de.fliplearn.backend.dto.CurrentUserResponse;
import de.fliplearn.backend.dto.UserProfileResponse;
import de.fliplearn.backend.entity.AppUser;
import de.fliplearn.backend.repository.AppUserRepository;
import de.fliplearn.backend.repository.FlashcardRepository;
import de.fliplearn.backend.repository.FlashcardSetRepository;
import de.fliplearn.backend.study.repository.StudySessionRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final AppUserRepository appUserRepository;
    private final FlashcardSetRepository flashcardSetRepository;
    private final FlashcardRepository flashcardRepository;
    private final StudySessionRepository studySessionRepository;

    public UserService(
            AppUserRepository appUserRepository,
            FlashcardSetRepository flashcardSetRepository,
            FlashcardRepository flashcardRepository,
            StudySessionRepository studySessionRepository
    ) {
        this.appUserRepository = appUserRepository;
        this.flashcardSetRepository = flashcardSetRepository;
        this.flashcardRepository = flashcardRepository;
        this.studySessionRepository = studySessionRepository;
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(
            String email
    ) {
        AppUser user = findUser(email);

        return new CurrentUserResponse(
                user.getId(),
                user.getDisplayName(),
                user.getEmail(),
                user.getRole().name(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(
            String email
    ) {
        AppUser user = findUser(email);

        long totalSets =
                flashcardSetRepository
                        .countByOwnerEmailIgnoreCase(email);

        long totalCards =
                flashcardRepository
                        .countByFlashcardSetOwnerEmailIgnoreCase(email);

        long favoriteSets =
                flashcardSetRepository
                        .countByOwnerEmailIgnoreCaseAndFavoriteTrue(email);

        long completedSessions =
                studySessionRepository
                        .countByUserEmailIgnoreCaseAndCompletedAtIsNotNull(
                                email
                        );

        long totalReviews =
                flashcardRepository
                        .sumTotalReviewsByOwnerEmail(email);

        long correctReviews =
                flashcardRepository
                        .sumCorrectReviewsByOwnerEmail(email);

        int accuracy = calculateAccuracy(
                correctReviews,
                totalReviews
        );

        return new UserProfileResponse(
                user.getId(),
                user.getDisplayName(),
                user.getEmail(),
                user.getRole().name(),
                user.getCreatedAt(),
                totalSets,
                totalCards,
                favoriteSets,
                completedSessions,
                totalReviews,
                correctReviews,
                accuracy
        );
    }

    private AppUser findUser(
            String email
    ) {
        return appUserRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Benutzer wurde nicht gefunden."
                        )
                );
    }

    private int calculateAccuracy(
            long correctReviews,
            long totalReviews
    ) {
        if (totalReviews == 0) {
            return 0;
        }

        return Math.round(
                correctReviews * 100.0f /
                        totalReviews
        );
    }
}