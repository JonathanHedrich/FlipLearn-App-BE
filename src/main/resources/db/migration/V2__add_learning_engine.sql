ALTER TABLE flashcards
    ADD COLUMN ease_factor NUMERIC(4, 2) NOT NULL DEFAULT 2.50,
    ADD COLUMN interval_days INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN repetitions INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN next_review_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN last_reviewed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN total_reviews INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN correct_reviews INTEGER NOT NULL DEFAULT 0;

ALTER TABLE flashcards
    ADD CONSTRAINT chk_flashcards_ease_factor
        CHECK (ease_factor >= 1.30),

    ADD CONSTRAINT chk_flashcards_interval_days
        CHECK (interval_days >= 0),

    ADD CONSTRAINT chk_flashcards_repetitions
        CHECK (repetitions >= 0),

    ADD CONSTRAINT chk_flashcards_review_counts
        CHECK (
            total_reviews >= 0
            AND correct_reviews >= 0
            AND correct_reviews <= total_reviews
        );

CREATE TABLE study_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    set_id BIGINT NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    total_cards INTEGER NOT NULL DEFAULT 0,
    correct_answers INTEGER NOT NULL DEFAULT 0,
    incorrect_answers INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT fk_study_sessions_user
        FOREIGN KEY (user_id)
        REFERENCES app_users (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_study_sessions_set
        FOREIGN KEY (set_id)
        REFERENCES flashcard_sets (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_study_sessions_counts
        CHECK (
            total_cards >= 0
            AND correct_answers >= 0
            AND incorrect_answers >= 0
            AND correct_answers + incorrect_answers <= total_cards
        )
);

CREATE TABLE study_reviews (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    card_id BIGINT NOT NULL,
    rating VARCHAR(20) NOT NULL,
    answered_correctly BOOLEAN NOT NULL,
    reviewed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    previous_interval_days INTEGER NOT NULL,
    new_interval_days INTEGER NOT NULL,
    previous_ease_factor NUMERIC(4, 2) NOT NULL,
    new_ease_factor NUMERIC(4, 2) NOT NULL,

    CONSTRAINT fk_study_reviews_session
        FOREIGN KEY (session_id)
        REFERENCES study_sessions (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_study_reviews_card
        FOREIGN KEY (card_id)
        REFERENCES flashcards (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_study_reviews_rating
        CHECK (rating IN ('AGAIN', 'HARD', 'GOOD', 'EASY'))
);

CREATE INDEX idx_flashcards_next_review_at
    ON flashcards (next_review_at);

CREATE INDEX idx_study_sessions_user_id
    ON study_sessions (user_id);

CREATE INDEX idx_study_sessions_set_id
    ON study_sessions (set_id);

CREATE INDEX idx_study_reviews_session_id
    ON study_reviews (session_id);

CREATE INDEX idx_study_reviews_card_id
    ON study_reviews (card_id);