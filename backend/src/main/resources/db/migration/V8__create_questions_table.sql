CREATE TABLE questions (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL,
    statement TEXT NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    time_limit_seconds INT NOT NULL DEFAULT 10,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_questions_category
        FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE INDEX idx_questions_category_id ON questions (category_id);
