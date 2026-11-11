package com.barbershop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelAppointmentRequest(
        @NotBlank(message = "O motivo do cancelamento é obrigatório.")
        @Size(max = 255)
        @Schema(example = "Cliente solicitou cancelamento via WhatsApp")
        String reason
) {}
