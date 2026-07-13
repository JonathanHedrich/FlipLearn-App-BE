package de.fliplearn.backend.study.entity;

import de.fliplearn.backend.entity.Flashcard;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "study_reviews")
public class StudyReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "session_id",
            nullable = false
    )
    private StudySession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "card_id",
            nullable = false
    )
    private Flashcard card;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "rating",
            nullable = false,
            length = 20
    )
    private StudyRating rating;

    @Column(
            name = "answered_correctly",
            nullable = false
    )
    private boolean answeredCorrectly;

    @Column(
            name = "reviewed_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime reviewedAt;

    @Column(
            name = "previous_interval_days",
            nullable = false
    )
    private int previousIntervalDays;

    @Column(
            name = "new_interval_days",
            nullable = false
    )
    private int newIntervalDays;

    @Column(
            name = "previous_ease_factor",
            nullable = false,
            precision = 4,
            scale = 2
    )
    private BigDecimal previousEaseFactor;

    @Column(
            name = "new_ease_factor",
            nullable = false,
            precision = 4,
            scale = 2
    )
    private BigDecimal newEaseFactor;

    protected StudyReview() {
    }

    public StudyReview(
            StudySession session,
            Flashcard card,
            StudyRating rating,
            boolean answeredCorrectly,
            int previousIntervalDays,
            int newIntervalDays,
            BigDecimal previousEaseFactor,
            BigDecimal newEaseFactor
    ) {
        this.session = session;
        this.card = card;
        this.rating = rating;
        this.answeredCorrectly = answeredCorrectly;
        this.previousIntervalDays = previousIntervalDays;
        this.newIntervalDays = newIntervalDays;
        this.previousEaseFactor = previousEaseFactor;
        this.newEaseFactor = newEaseFactor;
    }

    @PrePersist
    void onCreate() {
        if (reviewedAt == null) {
            reviewedAt = OffsetDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public StudySession getSession() {
        return session;
    }

    public Flashcard getCard() {
        return card;
    }

    public StudyRating getRating() {
        return rating;
    }

    public boolean isAnsweredCorrectly() {
        return answeredCorrectly;
    }

    public OffsetDateTime getReviewedAt() {
        return reviewedAt;
    }

    public int getPreviousIntervalDays() {
        return previousIntervalDays;
    }

    public int getNewIntervalDays() {
        return newIntervalDays;
    }

    public BigDecimal getPreviousEaseFactor() {
        return previousEaseFactor;
    }

    public BigDecimal getNewEaseFactor() {
        return newEaseFactor;
    }
}