package de.fliplearn.backend.entity;

import jakarta.persistence.*;
import de.fliplearn.backend.category.entity.FlashcardCategory;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import de.fliplearn.backend.category.entity.FlashcardCategory;

@Entity
@Table(name = "flashcard_sets")
public class FlashcardSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "owner_id",
            nullable = false
    )
    private AppUser owner;

    @Column(
            name = "title",
            nullable = false,
            length = 100
    )
    private String title;

    @Column(
            name = "description",
            length = 500
    )
    private String description;

    @Column(
            name = "color",
            nullable = false,
            length = 30
    )
    private String color = "blue";

    @Column(
            name = "favorite",
            nullable = false
    )
    private boolean favorite;

    @Column(
            name = "progress",
            nullable = false
    )
    private int progress;

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

    @OneToMany(
            mappedBy = "flashcardSet",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Flashcard> cards = new ArrayList<>();

    protected FlashcardSet() {
    }

    public int getCardCount() {
        return cards.size();
    }

    public FlashcardSet(
            AppUser owner,
            String title,
            String description,
            String color
    ) {
        this.owner = owner;
        this.title = title;
        this.description = description;
        this.color = color;
        this.favorite = false;
        this.progress = 0;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private FlashcardCategory category;

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public AppUser getOwner() {
        return owner;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getColor() {
        return color;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public int getProgress() {
        return progress;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<Flashcard> getCards() {
        return cards;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public FlashcardCategory getCategory() {
        return category;
    }

    public void setCategory(
            FlashcardCategory category
    ) {
        this.category = category;
    }
}