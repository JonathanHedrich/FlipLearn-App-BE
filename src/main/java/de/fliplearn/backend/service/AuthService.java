package de.fliplearn.backend.service;

import de.fliplearn.backend.dto.GoogleLoginRequest;
import de.fliplearn.backend.dto.GoogleUser;
import de.fliplearn.backend.dto.LoginRequest;
import de.fliplearn.backend.dto.LoginResponse;
import de.fliplearn.backend.dto.RegisterRequest;
import de.fliplearn.backend.dto.RegisterResponse;
import de.fliplearn.backend.entity.AppUser;
import de.fliplearn.backend.entity.AuthProvider;
import de.fliplearn.backend.exception.InvalidCredentialsException;
import de.fliplearn.backend.exception.ResourceConflictException;
import de.fliplearn.backend.repository.AppUserRepository;
import de.fliplearn.backend.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private static final int USERNAME_MAX_LENGTH = 100;

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifierService googleTokenVerifierService;

    public AuthService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            GoogleTokenVerifierService googleTokenVerifierService
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleTokenVerifierService = googleTokenVerifierService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        if (appUserRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResourceConflictException(
                    "Für diese E-Mail-Adresse existiert bereits ein Konto."
            );
        }

        String username = normalizedEmail;

        if (appUserRepository.existsByUsernameIgnoreCase(username)) {
            throw new ResourceConflictException(
                    "Dieser Benutzername ist bereits vergeben."
            );
        }

        String passwordHash = passwordEncoder.encode(
                request.password()
        );

        AppUser user = new AppUser(
                normalizedEmail,
                username,
                passwordHash,
                request.displayName().trim()
        );

        AppUser savedUser = appUserRepository.save(user);

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getDisplayName(),
                savedUser.getEmail(),
                savedUser.getRole().name(),
                savedUser.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(
                request.email()
        );

        AppUser user = appUserRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isEnabled()) {
            throw new InvalidCredentialsException();
        }

        if (
                user.getAuthProvider() != AuthProvider.LOCAL ||
                        !user.hasLocalPassword()
        ) {
            throw new InvalidCredentialsException();
        }

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        );

        if (!passwordMatches) {
            throw new InvalidCredentialsException();
        }

        return createLoginResponse(user);
    }

    @Transactional
    public LoginResponse googleLogin(
            GoogleLoginRequest request
    ) {
        GoogleUser googleUser = googleTokenVerifierService.verify(
                request.idToken()
        );

        AppUser user = appUserRepository
                .findByGoogleSubject(googleUser.subject())
                .map(existingUser ->
                        updateExistingGoogleUser(
                                existingUser,
                                googleUser
                        )
                )
                .orElseGet(() ->
                        createGoogleUser(googleUser)
                );

        if (!user.isEnabled()) {
            throw new InvalidCredentialsException();
        }

        return createLoginResponse(user);
    }

    private AppUser updateExistingGoogleUser(
            AppUser user,
            GoogleUser googleUser
    ) {
        if (!user.isGoogleAccount()) {
            throw new ResourceConflictException(
                    "Die Google-Anmeldung ist bereits einem anderen Kontotyp zugeordnet."
            );
        }

        if (!user.getEmail().equalsIgnoreCase(googleUser.email())) {
            user.setEmail(googleUser.email());
        }

        user.updateGoogleProfile(
                googleUser.displayName(),
                googleUser.pictureUrl(),
                googleUser.emailVerified()
        );

        return appUserRepository.save(user);
    }

    private AppUser createGoogleUser(
            GoogleUser googleUser
    ) {
        AppUser existingEmailUser = appUserRepository
                .findByEmailIgnoreCase(googleUser.email())
                .orElse(null);

        if (existingEmailUser != null) {
            throw new ResourceConflictException(
                    "Für diese E-Mail-Adresse existiert bereits ein Konto. "
                            + "Melde dich zunächst mit E-Mail und Passwort an."
            );
        }

        String username = generateUniqueUsername(
                googleUser.email()
        );

        AppUser newUser = AppUser.createGoogleUser(
                googleUser.email(),
                username,
                googleUser.displayName(),
                googleUser.subject(),
                googleUser.pictureUrl(),
                googleUser.emailVerified()
        );

        return appUserRepository.save(newUser);
    }

    private String generateUniqueUsername(String email) {
        String baseUsername = extractUsernameBase(email);

        if (!appUserRepository.existsByUsernameIgnoreCase(baseUsername)) {
            return baseUsername;
        }

        int suffix = 2;

        while (suffix < 100_000) {
            String suffixValue = String.valueOf(suffix);

            int allowedBaseLength =
                    USERNAME_MAX_LENGTH - suffixValue.length();

            String shortenedBase = baseUsername.substring(
                    0,
                    Math.min(
                            baseUsername.length(),
                            allowedBaseLength
                    )
            );

            String candidate = shortenedBase + suffixValue;

            if (
                    !appUserRepository.existsByUsernameIgnoreCase(
                            candidate
                    )
            ) {
                return candidate;
            }

            suffix++;
        }

        throw new IllegalStateException(
                "Es konnte kein eindeutiger Benutzername erzeugt werden."
        );
    }

    private String extractUsernameBase(String email) {
        int separatorIndex = email.indexOf('@');

        String localPart = separatorIndex > 0
                ? email.substring(0, separatorIndex)
                : email;

        String normalizedUsername = localPart
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "")
                .replaceAll("^[._-]+", "")
                .replaceAll("[._-]+$", "");

        if (normalizedUsername.isBlank()) {
            normalizedUsername = "user";
        }

        return normalizedUsername.substring(
                0,
                Math.min(
                        normalizedUsername.length(),
                        USERNAME_MAX_LENGTH
                )
        );
    }

    private LoginResponse createLoginResponse(AppUser user) {
        String accessToken = jwtService.generateAccessToken(user);

        long expiresIn = jwtService.getExpirationSeconds();

        return new LoginResponse(
                accessToken,
                "Bearer",
                expiresIn,
                user.getId(),
                user.getDisplayName(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    private String normalizeEmail(String email) {
        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}