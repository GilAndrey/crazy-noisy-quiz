CREATE TABLE quiz_rooms (
    id UUID PRIMARY KEY,
    code VARCHAR(6) NOT NULL UNIQUE,
    owner_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    max_players INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ,

    CONSTRAINT fk_quiz_rooms_owner
        FOREIGN KEY (owner_id) REFERENCES users (id)
);

CREATE INDEX idx_quiz_rooms_code ON quiz_rooms (code);
