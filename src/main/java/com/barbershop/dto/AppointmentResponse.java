package com.barbershop.dto;

import com.barbershop.domain.AppointmentStatus;
import com.barbershop.domain.Appointment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentResponse(
        Long id,
        String clientName,
        String clientPhone,
        Long serviceId,
        String serviceName,
        BigDecimal servicePrice,
        LocalDate appointmentDate,
        LocalTime startTime,
        AppointmentStatus status
) {
    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getClientName(),
                appointment.getClientPhone(),
                appointment.getService().getId(),
                appointment.getService().getName(),
                appointment.getService().getPrice(),
                appointment.getAppointmentDate(),
                appointment.getStartTime(),
                appointment.getStatus()
        );
    }
}
