CREATE TABLE question_options (
    id UUID PRIMARY KEY,
    question_id UUID NOT NULL,
    option_text VARCHAR(500) NOT NULL,
    option_order INT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_question_options_question
        FOREIGN KEY (question_id) REFERENCES questions (id),
    CONSTRAINT uq_question_options_question_order
        UNIQUE (question_id, option_order)
);

CREATE INDEX idx_question_options_question_id
    ON question_options (question_id);
