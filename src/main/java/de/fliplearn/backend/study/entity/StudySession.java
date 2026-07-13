package de.fliplearn.backend.study.entity;

import de.fliplearn.backend.entity.AppUser;
import de.fliplearn.backend.entity.FlashcardSet;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "study_sessions")
public class StudySession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "set_id",
            nullable = false
    )
    private FlashcardSet flashcardSet;

    @Column(
            name = "started_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(
            name = "total_cards",
            nullable = false
    )
    private int totalCards;

    @Column(
            name = "correct_answers",
            nullable = false
    )
    private int correctAnswers;

    @Column(
            name = "incorrect_answers",
            nullable = false
    )
    private int incorrectAnswers;

    @OneToMany(
            mappedBy = "session",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<StudyReview> reviews = new ArrayList<>();

    protected StudySession() {
    }

    public StudySession(
            AppUser user,
            FlashcardSet flashcardSet,
            int totalCards
    ) {
        this.user = user;
        this.flashcardSet = flashcardSet;
        this.totalCards = totalCards;
        this.correctAnswers = 0;
        this.incorrectAnswers = 0;
    }

    @PrePersist
    void onCreate() {
        if (startedAt == null) {
            startedAt = OffsetDateTime.now();
        }
    }

    public void recordAnswer(boolean correct) {
        if (correct) {
            correctAnswers++;
        } else {
            incorrectAnswers++;
        }
    }

    public void complete() {
        if (completedAt == null) {
            completedAt = OffsetDateTime.now();
        }
    }

    public boolean isComplete() {
        return completedAt != null;
    }

    public int getAnsweredCards() {
        return correctAnswers + incorrectAnswers;
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public FlashcardSet getFlashcardSet() {
        return flashcardSet;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public int getTotalCards() {
        return totalCards;
    }

    public int getCorrectAnswers() {
        return correctAnswers;
    }

    public int getIncorrectAnswers() {
        return incorrectAnswers;
    }

    public List<StudyReview> getReviews() {
        return reviews;
    }
}