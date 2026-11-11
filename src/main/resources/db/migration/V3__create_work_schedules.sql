-- V3: Expediente padrão semanal do barbeiro
-- work_day = FALSE representa folga (horários devem ser NULL).
-- work_day = TRUE exige end_time > start_time e intervalo opcional consistente.

CREATE TABLE work_schedules (
    id          BIGSERIAL   PRIMARY KEY,
    day_of_week VARCHAR(10) NOT NULL,
    start_time  TIME,
    end_time    TIME,
    break_start TIME,
    break_end   TIME,
    work_day    BOOLEAN     NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_work_schedule_day UNIQUE (day_of_week),
    CONSTRAINT chk_work_schedule_consistency
        CHECK (
            (work_day = FALSE AND start_time IS NULL AND end_time IS NULL AND break_start IS NULL AND break_end IS NULL)
            OR
            (work_day = TRUE AND start_time IS NOT NULL AND end_time IS NOT NULL AND end_time > start_time
             AND (
                 (break_start IS NULL AND break_end IS NULL)
                 OR
                 (break_start IS NOT NULL AND break_end IS NOT NULL
                  AND break_end > break_start
                  AND break_start >= start_time
                  AND break_end <= end_time)
             ))
        )
);

COMMENT ON TABLE work_schedules IS 'Expediente semanal padrão do barbeiro';
