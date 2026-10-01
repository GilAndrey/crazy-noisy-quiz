CREATE TABLE match_participants (
    id UUID PRIMARY KEY,
    match_id UUID NOT NULL,
    user_id UUID NOT NULL,
    total_points INT NOT NULL DEFAULT 0,
    correct_answers INT NOT NULL DEFAULT 0,
    position INT,
    joined_at TIMESTAMPTZ NOT NULL,
    disconnected_at TIMESTAMPTZ,

    CONSTRAINT fk_match_participants_match
        FOREIGN KEY (match_id) REFERENCES matches (id),
    CONSTRAINT fk_match_participants_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_match_participants_match_user
        UNIQUE (match_id, user_id)
);

CREATE INDEX idx_match_participants_user_id ON match_participants (user_id);

CREATE TABLE match_rounds (
    id UUID PRIMARY KEY,
    match_id UUID NOT NULL,
    question_id UUID NOT NULL,
    round_number INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,

    CONSTRAINT fk_match_rounds_match
        FOREIGN KEY (match_id) REFERENCES matches (id),
    CONSTRAINT fk_match_rounds_question
        FOREIGN KEY (question_id) REFERENCES questions (id),
    CONSTRAINT uq_match_rounds_match_number
        UNIQUE (match_id, round_number)
);

CREATE INDEX idx_match_rounds_question_id ON match_rounds (question_id);
