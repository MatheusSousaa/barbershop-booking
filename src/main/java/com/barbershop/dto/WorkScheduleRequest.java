package com.barbershop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record WorkScheduleRequest(
        @NotNull(message = "O dia da semana é obrigatório.")
        @Schema(example = "MONDAY")
        DayOfWeek dayOfWeek,

        @Schema(example = "08:00:00")
        LocalTime startTime,

        @Schema(example = "18:00:00")
        LocalTime endTime,

        @Schema(example = "12:00:00")
        LocalTime breakStart,

        @Schema(example = "13:00:00")
        LocalTime breakEnd,

        @NotNull(message = "O indicador de dia de trabalho é obrigatório.")
        @Schema(example = "true")
        Boolean workDay
) {}
