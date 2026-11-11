package com.barbershop.notification;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AppointmentNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(AppointmentNotificationListener.class);

    private final NotificationService notificationService;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAppointmentCreated(AppointmentCreatedEvent event) {
        try {
            notificationService.notifyAppointmentCreated(event.appointment());
        } catch (RuntimeException exception) {
            log.error("Falha ao notificar novo agendamento id={} (erro: {})",
                    event.appointment().appointmentId(), exception.getClass().getSimpleName());
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAppointmentCancelled(AppointmentCancelledEvent event) {
        try {
            notificationService.notifyAppointmentCancelled(event.appointment());
        } catch (RuntimeException exception) {
            log.error("Falha ao notificar cancelamento de agendamento id={} (erro: {})",
                    event.appointment().appointmentId(), exception.getClass().getSimpleName());
        }
    }
}
