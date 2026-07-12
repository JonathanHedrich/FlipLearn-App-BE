CREATE TABLE app_users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(150) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'USER',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE flashcard_sets (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    folder VARCHAR(100),
    color VARCHAR(30) NOT NULL DEFAULT 'blue',
    favorite BOOLEAN NOT NULL DEFAULT FALSE,
    progress INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_flashcard_sets_owner
        FOREIGN KEY (owner_id)
        REFERENCES app_users (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_flashcard_sets_progress
        CHECK (progress BETWEEN 0 AND 100)
);

CREATE TABLE flashcards (
    id BIGSERIAL PRIMARY KEY,
    set_id BIGINT NOT NULL,
    front TEXT NOT NULL,
    back TEXT NOT NULL,
    favorite BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_flashcards_set
        FOREIGN KEY (set_id)
        REFERENCES flashcard_sets (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_flashcard_sets_owner_id
    ON flashcard_sets (owner_id);

CREATE INDEX idx_flashcards_set_id
    ON flashcards (set_id);