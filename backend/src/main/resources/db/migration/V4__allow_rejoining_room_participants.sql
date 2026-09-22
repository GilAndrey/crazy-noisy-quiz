ALTER TABLE room_participants
    DROP CONSTRAINT uk_room_participants_room_user;

CREATE UNIQUE INDEX uk_room_participants_active_room_user
    ON room_participants (room_id, user_id)
    WHERE left_at IS NULL;
