package com.barbershop.notification;

import com.barbershop.domain.Appointment;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Snapshot imutável do agendamento, seguro para consumo depois do commit e fora da transação.
 */
public record AppointmentNotification(
        Long appointmentId,
        String clientName,
        String clientPhone,
        String serviceName,
        LocalDate appointmentDate,
        LocalTime startTime,
        String cancellationReason
) {
    public static AppointmentNotification from(Appointment appointment, String cancellationReason) {
        return new AppointmentNotification(
                appointment.getId(),
                appointment.getClientName(),
                appointment.getClientPhone(),
                appointment.getService().getName(),
                appointment.getAppointmentDate(),
                appointment.getStartTime(),
                cancellationReason
        );
    }
}
