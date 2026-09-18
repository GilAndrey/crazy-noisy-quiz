CREATE TABLE room_participants (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL,
    user_id UUID NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL,
    left_at TIMESTAMPTZ,

    CONSTRAINT fk_room_participants_room
        FOREIGN KEY (room_id) REFERENCES quiz_rooms (id),

    CONSTRAINT fk_room_participants_user
        FOREIGN KEY (user_id) REFERENCES users (id),

    CONSTRAINT uk_room_participants_room_user
        UNIQUE (room_id, user_id)
);

CREATE INDEX idx_room_participants_room_id
    ON room_participants (room_id);

CREATE INDEX idx_room_participants_user_id
    ON room_participants (user_id);
