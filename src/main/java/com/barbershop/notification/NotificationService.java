package com.barbershop.notification;

/**
 * Contrato de notificação do sistema.
 * Desacopla o disparo de mensagens para permitir trocar a implementação
 * (Log, E-mail, WhatsApp/Evolution API, etc.) sem alterar o serviço de agendamento.
 */
public interface NotificationService {

    void notifyAppointmentCreated(AppointmentNotification appointment);

    void notifyAppointmentCancelled(AppointmentNotification appointment);
}
