CREATE TABLE matches (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_rounds INT NOT NULL DEFAULT 10,
    current_round_number INT,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_matches_room
        FOREIGN KEY (room_id) REFERENCES quiz_rooms (id)
);

CREATE INDEX idx_matches_room_id ON matches (room_id);

CREATE TABLE match_categories (
    match_id UUID NOT NULL,
    category_id UUID NOT NULL,

    CONSTRAINT pk_match_categories
        PRIMARY KEY (match_id, category_id),
    CONSTRAINT fk_match_categories_match
        FOREIGN KEY (match_id) REFERENCES matches (id),
    CONSTRAINT fk_match_categories_category
        FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE INDEX idx_match_categories_category_id ON match_categories (category_id);
