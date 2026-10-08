CREATE TABLE match_answers (
    id UUID PRIMARY KEY,
    round_id UUID NOT NULL,
    participant_id UUID NOT NULL,
    option_id UUID NOT NULL,
    answered_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_match_answers_round
        FOREIGN KEY (round_id) REFERENCES match_rounds (id),
    CONSTRAINT fk_match_answers_participant
        FOREIGN KEY (participant_id) REFERENCES match_participants (id),
    CONSTRAINT fk_match_answers_option
        FOREIGN KEY (option_id) REFERENCES question_options (id),
    -- Mesmo com duas requisições simultâneas, só uma resposta pode ser salva.
    CONSTRAINT uq_match_answers_round_participant
        UNIQUE (round_id, participant_id)
);

CREATE INDEX idx_match_answers_participant_id ON match_answers (participant_id);
CREATE INDEX idx_match_answers_option_id ON match_answers (option_id);
