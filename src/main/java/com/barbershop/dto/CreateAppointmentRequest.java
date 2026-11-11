package com.barbershop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/** Dados de entrada para criação de agendamento pelo cliente. */
public record CreateAppointmentRequest(
        @NotNull(message = "O serviço é obrigatório.")
        @Schema(example = "1")
        Long serviceId,

        @NotNull(message = "A data é obrigatória.")
        @Schema(example = "2026-11-10")
        LocalDate date,

        @NotNull(message = "O horário é obrigatório.")
        @Schema(example = "09:00:00")
        LocalTime startTime,

        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
        @Schema(example = "Cliente Exemplo")
        String clientName,

        @NotBlank(message = "O telefone é obrigatório.")
        @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres.")
        @Schema(example = "00000000000")
        String clientPhone
) {}
