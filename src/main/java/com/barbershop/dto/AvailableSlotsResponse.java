package com.barbershop.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record AvailableSlotsResponse(
        LocalDate date,
        List<LocalTime> availableSlots
) {}
