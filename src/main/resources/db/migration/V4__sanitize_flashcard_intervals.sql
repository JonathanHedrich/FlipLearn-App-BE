UPDATE flashcards
SET interval_days = 3650
WHERE interval_days > 3650;

UPDATE flashcards
SET interval_days = 0
WHERE interval_days < 0;

UPDATE flashcards
SET next_review_at =
    CURRENT_TIMESTAMP + INTERVAL '3650 days'
WHERE next_review_at >
    CURRENT_TIMESTAMP + INTERVAL '3650 days';

ALTER TABLE flashcards
DROP CONSTRAINT IF EXISTS chk_flashcards_interval_days;

ALTER TABLE flashcards
ADD CONSTRAINT chk_flashcards_interval_days
CHECK (
    interval_days >= 0
    AND interval_days <= 3650
);