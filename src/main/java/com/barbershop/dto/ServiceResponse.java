package com.barbershop.dto;

import java.math.BigDecimal;

public record ServiceResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String photoUrl
) {}
