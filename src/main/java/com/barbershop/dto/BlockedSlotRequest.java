package com.barbershop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record BlockedSlotRequest(
        @NotNull(message = "A data é obrigatória.")
        @Schema(example = "2026-11-20")
        LocalDate date,

        @NotNull(message = "Informe se o bloqueio é de dia inteiro.")
        @Schema(example = "false")
        Boolean fullDay,

        @Schema(example = "14:00:00")
        LocalTime startTime,

        @Schema(example = "16:00:00")
        LocalTime endTime,

        @Size(max = 255)
        @Schema(example = "Consulta médica")
        String reason
) {}
