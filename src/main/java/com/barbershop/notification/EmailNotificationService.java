package com.barbershop.notification;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.format.DateTimeFormatter;

public class EmailNotificationService implements NotificationService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JavaMailSender mailSender;
    private final String senderAddress;
    private final String recipientAddress;

    public EmailNotificationService(JavaMailSender mailSender, String senderAddress, String recipientAddress) {
        this.mailSender = mailSender;
        this.senderAddress = senderAddress;
        this.recipientAddress = recipientAddress;
    }

    @Override
    public void notifyAppointmentCreated(AppointmentNotification appointment) {
        send("Novo agendamento confirmado", appointment, false);
    }

    @Override
    public void notifyAppointmentCancelled(AppointmentNotification appointment) {
        send("Agendamento cancelado", appointment, true);
    }

    private void send(String subject, AppointmentNotification appointment, boolean cancelled) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderAddress);
        message.setTo(recipientAddress);
        message.setSubject(subject);
        message.setText("""
                %s

                Cliente: %s
                Telefone: %s
                Serviço: %s
                Data: %s
                Horário: %s
                %s
                """.formatted(
                cancelled ? "Um agendamento foi cancelado." : "Um novo agendamento foi confirmado.",
                appointment.clientName(),
                appointment.clientPhone(),
                appointment.serviceName(),
                appointment.appointmentDate().format(DATE_FORMAT),
                appointment.startTime(),
                cancelled ? "Motivo: " + appointment.cancellationReason() : ""
        ));
        mailSender.send(message);
    }
}
