package com.barbershop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ServiceRequest(
        @NotBlank(message = "O nome do serviço é obrigatório.")
        @Size(max = 100)
        @Schema(example = "Corte Degradê")
        String name,

        @Size(max = 500)
        @Schema(example = "Corte moderno com acabamento na navalha")
        String description,

        @NotNull(message = "O preço é obrigatório.")
        @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero.")
        @Schema(example = "45.00")
        BigDecimal price
) {}
