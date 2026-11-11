-- ============================================================
-- V2: Tabela de serviços oferecidos pela barbearia
-- ============================================================
-- Sem coluna de duração nesta versão: todos os serviços ocupam
-- 1 slot de 30 min, configurado em application.properties.

CREATE TABLE services (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(100)    NOT NULL,
    description VARCHAR(500),
    price       NUMERIC(10, 2)  NOT NULL CHECK (price > 0),
    active      BOOLEAN         NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_service_name UNIQUE (name)
);

COMMENT ON TABLE  services         IS 'Serviços oferecidos pela barbearia (corte, barba, etc.)';
COMMENT ON COLUMN services.active  IS 'FALSE = soft-delete; preserva histórico de agendamentos';
COMMENT ON COLUMN services.price   IS 'Preço em reais (BRL)';
