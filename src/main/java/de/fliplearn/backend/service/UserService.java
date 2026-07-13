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

import de.fliplearn.backend.dto.ChangePasswordRequest;
import de.fliplearn.backend.exception.ResourceConflictException;
import org.springframework.security.crypto.password.PasswordEncoder;
import de.fliplearn.backend.dto.UpdateProfileRequest;
import de.fliplearn.backend.dto.ChangeEmailRequest;

@Service
public class UserService {

    private final AppUserRepository appUserRepository;
    private final FlashcardSetRepository flashcardSetRepository;
    private final FlashcardRepository flashcardRepository;
    private final StudySessionRepository studySessionRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            AppUserRepository appUserRepository,
            FlashcardSetRepository flashcardSetRepository,
            FlashcardRepository flashcardRepository,
            StudySessionRepository studySessionRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.appUserRepository = appUserRepository;
        this.flashcardSetRepository = flashcardSetRepository;
        this.flashcardRepository = flashcardRepository;
        this.studySessionRepository = studySessionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(
            String email
    ) {
        AppUser user = findUser(email);

        return new CurrentUserResponse(
                user.getId(),
                user.getDisplayName(),
                user.getUsername(),
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
                user.getUsername(),
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

    @Transactional
    public void changePassword(
            String email,
            ChangePasswordRequest request
    ) {
        AppUser user = findUser(email);

        if (
                !passwordEncoder.matches(
                        request.currentPassword(),
                        user.getPasswordHash()
                )
        ) {
            throw new ResourceConflictException(
                    "Das aktuelle Passwort ist falsch."
            );
        }

        if (
                passwordEncoder.matches(
                        request.newPassword(),
                        user.getPasswordHash()
                )
        ) {
            throw new ResourceConflictException(
                    "Das neue Passwort darf nicht dem aktuellen Passwort entsprechen."
            );
        }

        user.setPasswordHash(
                passwordEncoder.encode(
                        request.newPassword()
                )
        );

        appUserRepository.save(user);
    }
    @Transactional
    public CurrentUserResponse updateProfile(
            String email,
            UpdateProfileRequest request
    ) {
        AppUser user = findUser(email);

        String username = request.username().trim();
        String displayName = request.displayName().trim();

        boolean usernameChanged =
                !user.getUsername().equalsIgnoreCase(username);

        if (
                usernameChanged &&
                        appUserRepository.existsByUsernameIgnoreCase(username)
        ) {
            throw new ResourceConflictException(
                    "Dieser Benutzername ist bereits vergeben."
            );
        }

        user.setDisplayName(displayName);
        user.setUsername(username);

        AppUser savedUser =
                appUserRepository.save(user);

        return new CurrentUserResponse(
                savedUser.getId(),
                savedUser.getDisplayName(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole().name(),
                savedUser.isEnabled(),
                savedUser.getCreatedAt()
        );
    }

    @Transactional
    public void changeEmail(
            String currentEmail,
            ChangeEmailRequest request
    ) {
        AppUser user = findUser(currentEmail);

        String newEmail =
                request.newEmail()
                        .trim()
                        .toLowerCase();

        if (
                !passwordEncoder.matches(
                        request.currentPassword(),
                        user.getPasswordHash()
                )
        ) {
            throw new ResourceConflictException(
                    "Das aktuelle Passwort ist falsch."
            );
        }

        if (
                user.getEmail().equalsIgnoreCase(newEmail)
        ) {
            throw new ResourceConflictException(
                    "Die neue E-Mail-Adresse entspricht bereits deiner aktuellen E-Mail-Adresse."
            );
        }

        if (
                appUserRepository.existsByEmailIgnoreCase(
                        newEmail
                )
        ) {
            throw new ResourceConflictException(
                    "Diese E-Mail-Adresse wird bereits verwendet."
            );
        }

        user.setEmail(newEmail);

        appUserRepository.save(user);
    }
}