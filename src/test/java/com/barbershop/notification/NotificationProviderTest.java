package com.barbershop.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.env.MockEnvironment;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationProviderTest {
    private static final String CLIENT_PHONE = "00000000000";
    private static final AppointmentNotification APPOINTMENT = new AppointmentNotification(
            42L, "Cliente Teste", CLIENT_PHONE, "Corte", LocalDate.of(2026, 11, 12),
            LocalTime.of(14, 0), "Solicitado pelo cliente");

    @Test
    void usesLogProviderWhenSmtpIsNotConfigured() {
        NotificationService service = new NotificationConfiguration().notificationService(new MockEnvironment());

        assertInstanceOf(LogNotificationService.class, service);
    }

    @Test
    void usesEmailProviderWhenAllSmtpValuesAreConfigured() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("SMTP_HOST", "smtp.example.test")
                .withProperty("SMTP_PORT", "587")
                .withProperty("SMTP_USERNAME", "cliente@example.invalid")
                .withProperty("SMTP_PASSWORD", "not-a-real-password")
                .withProperty("SMTP_RECIPIENT", "smtp-user@example.invalid");

        NotificationService service = new NotificationConfiguration().notificationService(environment);

        assertInstanceOf(EmailNotificationService.class, service);
    }

    @Test
    void sendsCreationAndCancellationEmailsToConfiguredBarber() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailNotificationService service = new EmailNotificationService(
                mailSender, "notificacoes@example.invalid", "cliente@example.invalid");

        service.notifyAppointmentCreated(APPOINTMENT);
        service.notifyAppointmentCancelled(APPOINTMENT);

        var messages = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(2)).send(messages.capture());
        assertEquals("cliente@example.invalid", messages.getAllValues().get(0).getTo()[0]);
        assertEquals("Novo agendamento confirmado", messages.getAllValues().get(0).getSubject());
        assertEquals("Agendamento cancelado", messages.getAllValues().get(1).getSubject());
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void masksClientPhoneInFallbackLogs(CapturedOutput output) {
        new LogNotificationService().notifyAppointmentCreated(APPOINTMENT);

        assertTrue(output.getOut().contains("*******0000"));
        assertFalse(output.getOut().contains(CLIENT_PHONE));
    }
}
