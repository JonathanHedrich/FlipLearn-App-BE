package de.fliplearn.backend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import de.fliplearn.backend.dto.GoogleUser;
import de.fliplearn.backend.exception.InvalidCredentialsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Service
public class GoogleTokenVerifierService {

    private static final Logger log =
            LoggerFactory.getLogger(GoogleTokenVerifierService.class);

    private final GoogleIdTokenVerifier verifier;
    private final String googleClientId;

    public GoogleTokenVerifierService(
            @Value("${app.google.client-id:}") String googleClientId
    ) throws GeneralSecurityException, IOException {

        if (googleClientId == null || googleClientId.isBlank()) {
            throw new IllegalStateException(
                    "Die Konfiguration 'app.google.client-id' fehlt."
            );
        }

        this.googleClientId = googleClientId.trim();

        this.verifier = new GoogleIdTokenVerifier.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance()
        )
                .setAudience(Collections.singletonList(this.googleClientId))
                .build();

    }

    public GoogleUser verify(String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank()) {
            log.warn("Google-ID-Token fehlt oder ist leer.");
            throw new InvalidCredentialsException();
        }

        String normalizedToken = tokenValue.trim();

        try {
            /*
             * Das Token wird zunächst nur geparst, damit wir seine Claims
             * protokollieren können. Dadurch ist es noch nicht verifiziert.
             */
            GoogleIdToken parsedToken = GoogleIdToken.parse(
                    GsonFactory.getDefaultInstance(),
                    normalizedToken
            );

            GoogleIdToken.Payload parsedPayload = parsedToken.getPayload();

            logTokenDetails(parsedToken, parsedPayload);

            /*
             * Diese Prüfung umfasst unter anderem:
             * - Signatur
             * - Audience
             * - Issuer
             * - Ablaufzeit
             */
            boolean verified = verifier.verify(parsedToken);


            if (!verified) {
                logVerificationHints(parsedToken, parsedPayload);
                throw new InvalidCredentialsException();
            }

            String subject = parsedPayload.getSubject();
            String email = parsedPayload.getEmail();
            String displayName = parsedPayload.get("name") instanceof String name
                    ? name
                    : null;
            String pictureUrl = parsedPayload.get("picture") instanceof String picture
                    ? picture
                    : null;
            boolean emailVerified = Boolean.TRUE.equals(
                    parsedPayload.getEmailVerified()
            );

            if (subject == null || subject.isBlank()) {
                log.warn("Das verifizierte Google-ID-Token enthält kein Subject.");
                throw new InvalidCredentialsException();
            }

            if (email == null || email.isBlank()) {
                log.warn("Das verifizierte Google-ID-Token enthält keine E-Mail-Adresse.");
                throw new InvalidCredentialsException();
            }

            return new GoogleUser(
                    subject,
                    email,
                    displayName,
                    pictureUrl,
                    emailVerified
            );

        } catch (GeneralSecurityException exception) {
            log.error(
                    "Kryptografischer Fehler bei der Google-Tokenprüfung.",
                    exception
            );

            throw new InvalidCredentialsException();

        } catch (IOException exception) {
            log.error(
                    "Google-Zertifikate konnten nicht geladen oder geprüft werden.",
                    exception
            );

            throw new InvalidCredentialsException();

        } catch (IllegalArgumentException exception) {
            log.error(
                    "Google-ID-Token konnte nicht geparst werden.",
                    exception
            );

            throw new InvalidCredentialsException();
        }
    }

    private void logTokenDetails(
            GoogleIdToken token,
            GoogleIdToken.Payload payload
    ) {
    }

    private void logVerificationHints(
            GoogleIdToken token,
            GoogleIdToken.Payload payload
    ) {
        List<String> audiences = payload.getAudienceAsList();

        boolean audienceMatches =
                audiences != null && audiences.contains(googleClientId);

        String issuer = payload.getIssuer();

        boolean issuerMatches =
                "accounts.google.com".equals(issuer)
                        || "https://accounts.google.com".equals(issuer);

        Instant now = Instant.now();
        Instant issuedAt = toInstant(payload.getIssuedAtTimeSeconds());
        Instant expiresAt = toInstant(payload.getExpirationTimeSeconds());

        boolean issuedAtPlausible =
                issuedAt == null || !issuedAt.isAfter(now.plusSeconds(300));

        boolean notExpired =
                expiresAt == null || expiresAt.isAfter(now.minusSeconds(300));

        String algorithm = token.getHeader().getAlgorithm();
        String keyId = token.getHeader().getKeyId();

        log.warn(
                "Google-ID-Token wurde abgelehnt: audienceMatches={}, issuerMatches={}, issuedAtPlausible={}, notExpired={}, algorithm={}, keyIdPresent={}, tokenAudience={}, expectedAudience={}, issuer={}, issuedAt={}, expiresAt={}, serverTime={}",
                audienceMatches,
                issuerMatches,
                issuedAtPlausible,
                notExpired,
                algorithm,
                keyId != null && !keyId.isBlank(),
                audiences,
                googleClientId,
                issuer,
                issuedAt,
                expiresAt,
                now
        );

        if (
                audienceMatches
                        && issuerMatches
                        && issuedAtPlausible
                        && notExpired
        ) {
            log.warn(
                    "Audience, Issuer und Zeitangaben sehen korrekt aus. Wahrscheinlich scheitert die Signaturprüfung oder das Laden des passenden Google-Zertifikats."
            );
        }
    }

    private static Instant toInstant(Long seconds) {
        if (seconds == null) {
            return null;
        }

        return Instant.ofEpochSecond(seconds);
    }

    private static String maskClientId(String clientId) {
        String normalized = clientId.trim();

        if (normalized.length() <= 24) {
            return normalized;
        }

        return normalized.substring(0, 12)
                + "..."
                + normalized.substring(normalized.length() - 12);
    }

    private static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        int separatorIndex = email.indexOf('@');

        if (separatorIndex <= 1) {
            return "***";
        }

        String localPart = email.substring(0, separatorIndex);
        String domain = email.substring(separatorIndex);

        return localPart.substring(0, 1)
                + "***"
                + localPart.substring(localPart.length() - 1)
                + domain;
    }
}