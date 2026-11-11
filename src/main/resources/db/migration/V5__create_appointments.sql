-- ============================================================
-- V5: Tabela de agendamentos
-- ============================================================

CREATE TABLE appointments (
    id                  BIGSERIAL    PRIMARY KEY,
    client_name         VARCHAR(100) NOT NULL,
    client_phone        VARCHAR(20)  NOT NULL,
    service_id          BIGINT       NOT NULL REFERENCES services(id),
    appointment_date    DATE         NOT NULL,
    start_time          TIME         NOT NULL,
    status              VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE'
                            CHECK (status IN ('ACTIVE', 'CANCELLED')),
    cancellation_reason VARCHAR(255)
);

-- ============================================================
-- ÍNDICE ÚNICO PARCIAL — regra de negócio central
-- ============================================================
-- Garante que apenas UM agendamento ATIVO pode existir em cada
-- combinação (data, horário). Registros CANCELLED são ignorados
-- pelo índice, então cancelar um agendamento libera o horário
-- automaticamente sem precisar excluir o registro histórico.
--
-- Por que índice parcial e não UNIQUE simples na tabela?
--   UNIQUE simples incluiria os cancelados e impediria reagendar
--   o mesmo horário mesmo após o cancelamento.
--
-- A camada de serviço Java faz a checagem preventiva antes do
-- INSERT, mas este índice é a garantia final contra race conditions
-- (duas requisições simultâneas que passem pela checagem Java ao
-- mesmo tempo). A aplicação trata a DataIntegrityViolationException
-- resultante e retorna uma mensagem amigável.
-- ============================================================
CREATE UNIQUE INDEX uq_appointment_date_time_active
    ON appointments (appointment_date, start_time)
    WHERE status = 'ACTIVE';

-- Índices de suporte para listagens frequentes
CREATE INDEX idx_appointments_date_status
    ON appointments (appointment_date, status);

COMMENT ON TABLE  appointments                   IS 'Agendamentos dos clientes';
COMMENT ON COLUMN appointments.status            IS 'ACTIVE = vigente; CANCELLED = cancelado (mantido no histórico)';
COMMENT ON COLUMN appointments.cancellation_reason IS 'Motivo do cancelamento; NULL quando ACTIVE';
