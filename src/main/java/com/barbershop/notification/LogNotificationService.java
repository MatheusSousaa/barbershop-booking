package com.barbershop.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;

public class LogNotificationService implements NotificationService {
    private static final Logger log = LoggerFactory.getLogger(LogNotificationService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void notifyAppointmentCreated(AppointmentNotification appointment) {
        log.info("Novo agendamento: id={}, cliente={}, telefone={}, serviço={}, data={}, horário={}",
                appointment.appointmentId(), appointment.clientName(), maskPhone(appointment.clientPhone()),
                appointment.serviceName(), appointment.appointmentDate().format(DATE_FORMAT), appointment.startTime());
    }

    @Override
    public void notifyAppointmentCancelled(AppointmentNotification appointment) {
        log.info("Agendamento cancelado: id={}, cliente={}, telefone={}, serviço={}, data={}, horário={}, motivo={}",
                appointment.appointmentId(), appointment.clientName(), maskPhone(appointment.clientPhone()),
                appointment.serviceName(), appointment.appointmentDate().format(DATE_FORMAT), appointment.startTime(),
                appointment.cancellationReason());
    }

    private String maskPhone(String phone) {
        String digits = phone == null ? "" : phone.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return "****";
        }
        return "*".repeat(digits.length() - 4) + digits.substring(digits.length() - 4);
    }
}
