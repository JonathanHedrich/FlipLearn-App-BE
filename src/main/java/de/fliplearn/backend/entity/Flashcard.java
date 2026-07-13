package de.fliplearn.backend.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "flashcards")
public class Flashcard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "set_id",
            nullable = false
    )
    private FlashcardSet flashcardSet;

    @Column(
            name = "front",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String front;

    @Column(
            name = "back",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String back;

    @Column(
            name = "favorite",
            nullable = false
    )
    private boolean favorite;

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

    protected Flashcard() {
    }

    public Flashcard(
            FlashcardSet flashcardSet,
            String front,
            String back
    ) {
        this.flashcardSet = flashcardSet;
        this.front = front;
        this.back = back;
        this.favorite = false;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public FlashcardSet getFlashcardSet() {
        return flashcardSet;
    }

    public String getFront() {
        return front;
    }

    public String getBack() {
        return back;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setFront(String front) {
        this.front = front;
    }

    public void setBack(String back) {
        this.back = back;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    @Column(
            name = "ease_factor",
            nullable = false,
            precision = 4,
            scale = 2
    )
    private BigDecimal easeFactor = new BigDecimal("2.50");

    @Column(
            name = "interval_days",
            nullable = false
    )
    private int intervalDays;

    @Column(
            name = "repetitions",
            nullable = false
    )
    private int repetitions;

    @Column(name = "next_review_at")
    private OffsetDateTime nextReviewAt;

    @Column(name = "last_reviewed_at")
    private OffsetDateTime lastReviewedAt;

    @Column(
            name = "total_reviews",
            nullable = false
    )
    private int totalReviews;

    @Column(
            name = "correct_reviews",
            nullable = false
    )
    private int correctReviews;

    public BigDecimal getEaseFactor() {
        return easeFactor;
    }

    public int getIntervalDays() {
        return intervalDays;
    }

    public int getRepetitions() {
        return repetitions;
    }

    public OffsetDateTime getNextReviewAt() {
        return nextReviewAt;
    }

    public OffsetDateTime getLastReviewedAt() {
        return lastReviewedAt;
    }

    public int getTotalReviews() {
        return totalReviews;
    }

    public int getCorrectReviews() {
        return correctReviews;
    }

    public void updateLearningState(
            BigDecimal easeFactor,
            int intervalDays,
            int repetitions,
            OffsetDateTime nextReviewAt,
            OffsetDateTime lastReviewedAt,
            boolean answeredCorrectly
    ) {
        this.easeFactor = easeFactor;
        this.intervalDays = intervalDays;
        this.repetitions = repetitions;
        this.nextReviewAt = nextReviewAt;
        this.lastReviewedAt = lastReviewedAt;
        this.totalReviews += 1;

        if (answeredCorrectly) {
            this.correctReviews += 1;
        }
    }
}