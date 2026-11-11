package com.barbershop.dto;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresInMs,
        String displayName,
        String email
) {}
