package com.barbershop.domain;

/**
 * Mantém cancelamentos no histórico; o índice parcial reserva horários
 * exclusivamente para agendamentos ativos.
 */
public enum AppointmentStatus {
    ACTIVE,
    CANCELLED
}
