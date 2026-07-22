package de.fliplearn.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "app_users",
        indexes = {
                @Index(
                        name = "idx_app_users_google_subject",
                        columnList = "google_subject",
                        unique = true
                )
        }
)
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "email",
            nullable = false,
            unique = true,
            length = 255
    )
    private String email;

    @Column(
            name = "username",
            nullable = false,
            unique = true,
            length = 100
    )
    private String username;

    @Column(
            name = "password_hash",
            length = 255
    )
    private String passwordHash;

    @Column(
            name = "display_name",
            nullable = false,
            length = 150
    )
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "role",
            nullable = false,
            length = 30
    )
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "auth_provider",
            nullable = false,
            length = 20
    )
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(
            name = "google_subject",
            unique = true,
            length = 255
    )
    private String googleSubject;

    @Column(
            name = "email_verified",
            nullable = false
    )
    private boolean emailVerified;

    @Column(
            name = "profile_image_url",
            length = 2048
    )
    private String profileImageUrl;

    @Column(
            name = "enabled",
            nullable = false
    )
    private boolean enabled = true;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private OffsetDateTime updatedAt;

    protected AppUser() {
    }

    public AppUser(
            String email,
            String username,
            String passwordHash,
            String displayName
    ) {
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = UserRole.USER;
        this.authProvider = AuthProvider.LOCAL;
        this.googleSubject = null;
        this.emailVerified = false;
        this.profileImageUrl = null;
        this.enabled = true;
    }

    public static AppUser createGoogleUser(
            String email,
            String username,
            String displayName,
            String googleSubject,
            String profileImageUrl,
            boolean emailVerified
    ) {
        AppUser user = new AppUser();

        user.email = email;
        user.username = username;
        user.passwordHash = null;
        user.displayName = displayName;
        user.role = UserRole.USER;
        user.authProvider = AuthProvider.GOOGLE;
        user.googleSubject = googleSubject;
        user.emailVerified = emailVerified;
        user.profileImageUrl = profileImageUrl;
        user.enabled = true;

        return user;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.role == null) {
            this.role = UserRole.USER;
        }

        if (this.authProvider == null) {
            this.authProvider = AuthProvider.LOCAL;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserRole getRole() {
        return role;
    }

    public AuthProvider getAuthProvider() {
        return authProvider;
    }

    public String getGoogleSubject() {
        return googleSubject;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public void setAuthProvider(AuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    public void setGoogleSubject(String googleSubject) {
        this.googleSubject = googleSubject;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean hasLocalPassword() {
        return passwordHash != null && !passwordHash.isBlank();
    }

    public boolean isLocalAccount() {
        return authProvider == AuthProvider.LOCAL;
    }

    public boolean isGoogleAccount() {
        return authProvider == AuthProvider.GOOGLE;
    }

    public void linkGoogleAccount(
            String googleSubject,
            String profileImageUrl,
            boolean emailVerified
    ) {
        this.googleSubject = googleSubject;
        this.profileImageUrl = profileImageUrl;
        this.emailVerified = emailVerified;
        this.authProvider = AuthProvider.GOOGLE;
    }

    public void updateGoogleProfile(
            String displayName,
            String profileImageUrl,
            boolean emailVerified
    ) {
        if (displayName != null && !displayName.isBlank()) {
            this.displayName = displayName.trim();
        }

        this.profileImageUrl = profileImageUrl;
        this.emailVerified = emailVerified;
    }
}