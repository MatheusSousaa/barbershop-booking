package com.barbershop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Schema(example = "barbeiro@example.invalid")
        String email,

        @NotBlank(message = "A senha é obrigatória.")
        @Schema(example = "SenhaFicticia123")
        String password
) {}
