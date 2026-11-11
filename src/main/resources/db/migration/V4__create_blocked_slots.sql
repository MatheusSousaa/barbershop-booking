-- V4: Bloqueios pontuais de agenda
-- full_day = TRUE: horários devem ser NULL.
-- full_day = FALSE: end_time > start_time obrigatórios.

CREATE TABLE blocked_slots (
    id         BIGSERIAL    PRIMARY KEY,
    date       DATE         NOT NULL,
    full_day   BOOLEAN      NOT NULL DEFAULT FALSE,
    start_time TIME,
    end_time   TIME,
    reason     VARCHAR(255),

    CONSTRAINT chk_blocked_slot_times
        CHECK (
            (full_day = TRUE AND start_time IS NULL AND end_time IS NULL)
            OR
            (full_day = FALSE AND start_time IS NOT NULL AND end_time IS NOT NULL
             AND end_time > start_time)
        )
);

CREATE INDEX idx_blocked_slots_date ON blocked_slots (date);

COMMENT ON TABLE blocked_slots IS 'Bloqueios pontuais de agenda (feriados, folgas, compromissos)';
