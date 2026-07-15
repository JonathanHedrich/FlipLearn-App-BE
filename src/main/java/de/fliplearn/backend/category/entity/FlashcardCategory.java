package de.fliplearn.backend.category.entity;

import de.fliplearn.backend.entity.AppUser;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "flashcard_categories",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_category_owner_name",
                        columnNames = {
                                "owner_id",
                                "name"
                        }
                )
        }
)
public class FlashcardCategory {

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
            name = "name",
            nullable = false,
            length = 80
    )
    private String name;

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

    protected FlashcardCategory() {
    }

    public FlashcardCategory(
            AppUser owner,
            String name
    ) {
        this.owner = owner;
        this.name = name;
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

    public AppUser getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setName(String name) {
        this.name = name;
    }
}