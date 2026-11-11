-- ============================================================
-- V1: Tabela de barbeiros (usuários do painel administrativo)
-- ============================================================
-- Por que antes de tudo: nenhuma outra tabela depende desta,
-- mas ela pode futuramente receber FKs de work_schedules e
-- appointments quando houver múltiplos profissionais.

CREATE TABLE barbers (
    id           BIGSERIAL    PRIMARY KEY,
    username     VARCHAR(50)  NOT NULL,
    password     VARCHAR(255) NOT NULL,  -- hash BCrypt
    display_name VARCHAR(100) NOT NULL,
    email        VARCHAR(150),

    CONSTRAINT uq_barber_username UNIQUE (username)
);

COMMENT ON TABLE  barbers              IS 'Usuários do painel administrativo (barbeiros)';
COMMENT ON COLUMN barbers.password     IS 'Hash BCrypt — nunca texto puro';
COMMENT ON COLUMN barbers.display_name IS 'Nome de exibição na interface';
