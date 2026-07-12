package de.fliplearn.backend.service;

import de.fliplearn.backend.dto.RegisterRequest;
import de.fliplearn.backend.dto.RegisterResponse;
import de.fliplearn.backend.entity.AppUser;
import de.fliplearn.backend.exception.ResourceConflictException;
import de.fliplearn.backend.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.fliplearn.backend.dto.LoginRequest;
import de.fliplearn.backend.dto.LoginResponse;
import de.fliplearn.backend.exception.InvalidCredentialsException;
import de.fliplearn.backend.security.JwtService;

import java.util.Locale;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        if (appUserRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResourceConflictException(
                    "Für diese E-Mail-Adresse existiert bereits ein Konto."
            );
        }

        /*
         * In der ersten Version verwenden wir die normalisierte
         * E-Mail-Adresse auch intern als eindeutigen Benutzernamen.
         */
        String username = normalizedEmail;

        if (appUserRepository.existsByUsernameIgnoreCase(username)) {
            throw new ResourceConflictException(
                    "Dieser Benutzername ist bereits vergeben."
            );
        }

        String passwordHash = passwordEncoder.encode(request.password());

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

    private String normalizeEmail(String email) {
        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        AppUser user = appUserRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        );

        if (!passwordMatches || !user.isEnabled()) {
            throw new InvalidCredentialsException();
        }

        String accessToken =
                jwtService.generateAccessToken(user);

        return new LoginResponse(
                accessToken,
                "Bearer",
                jwtService.getExpirationSeconds(),
                user.getId(),
                user.getDisplayName(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}