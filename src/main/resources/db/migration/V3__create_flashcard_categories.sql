CREATE TABLE flashcard_categories (
    id BIGSERIAL PRIMARY KEY,

    owner_id BIGINT NOT NULL,

    name VARCHAR(80) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_flashcard_categories_owner
        FOREIGN KEY (owner_id)
        REFERENCES app_users(id)
        ON DELETE CASCADE
);

CREATE UNIQUE INDEX uk_flashcard_categories_owner_name_ci
    ON flashcard_categories (
        owner_id,
        LOWER(name)
    );

CREATE INDEX idx_flashcard_categories_owner_id
    ON flashcard_categories(owner_id);

ALTER TABLE flashcard_sets
    ADD COLUMN category_id BIGINT;

ALTER TABLE flashcard_sets
    ADD CONSTRAINT fk_flashcard_sets_category
        FOREIGN KEY (category_id)
        REFERENCES flashcard_categories(id)
        ON DELETE SET NULL;

CREATE INDEX idx_flashcard_sets_category_id
    ON flashcard_sets(category_id);