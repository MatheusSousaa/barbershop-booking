package com.barbershop.service;

import com.barbershop.domain.Appointment;
import com.barbershop.domain.AppointmentStatus;
import com.barbershop.domain.BarberService;
import com.barbershop.dto.AppointmentResponse;
import com.barbershop.dto.CreateAppointmentRequest;
import com.barbershop.exception.BusinessException;
import com.barbershop.exception.ScheduleConflictException;
import com.barbershop.notification.AppointmentCreatedEvent;
import com.barbershop.notification.AppointmentNotification;
import com.barbershop.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ServiceCatalogService serviceCatalogService;
    private final SlotCalculatorService slotCalculatorService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${barbershop.slot.duration-minutes}")
    private int slotDurationMinutes;

    @Transactional
    public AppointmentResponse createAppointment(CreateAppointmentRequest request) {
        validateSlotAlignment(request.startTime());
        BarberService service = serviceCatalogService.findActiveById(request.serviceId());
        ensureSlotAvailable(request);

        Appointment saved = saveAppointment(request, service);
        eventPublisher.publishEvent(new AppointmentCreatedEvent(AppointmentNotification.from(saved, null)));
        return AppointmentResponse.from(saved);
    }

    private void ensureSlotAvailable(CreateAppointmentRequest request) {
        List<LocalTime> availableSlots = slotCalculatorService.getAvailableSlots(request.date());
        if (!availableSlots.contains(request.startTime())) {
            if (isSlotBooked(request)) {
                throw new ScheduleConflictException("Esse horário acabou de ser reservado, escolha outro.");
            }
            throw new BusinessException("O horário selecionado não está disponível para agendamento.");
        }

        if (isSlotBooked(request)) {
            throw new ScheduleConflictException("Esse horário acabou de ser reservado, escolha outro.");
        }
    }

    private boolean isSlotBooked(CreateAppointmentRequest request) {
        return appointmentRepository.existsByAppointmentDateAndStartTimeAndStatus(
                request.date(), request.startTime(), AppointmentStatus.ACTIVE);
    }

    private Appointment saveAppointment(CreateAppointmentRequest request, BarberService service) {
        Appointment appointment = Appointment.builder()
                .clientName(request.clientName().trim())
                .clientPhone(request.clientPhone().trim())
                .service(service)
                .appointmentDate(request.date())
                .startTime(request.startTime())
                .status(AppointmentStatus.ACTIVE)
                .build();

        try {
            return appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException exception) {
            // O índice parcial resolve a corrida entre a checagem e a gravação.
            throw new ScheduleConflictException("Esse horário acabou de ser reservado, escolha outro.");
        }
    }

    private void validateSlotAlignment(LocalTime time) {
        if (time.getSecond() != 0 || time.getNano() != 0 || time.getMinute() % slotDurationMinutes != 0) {
            throw new BusinessException("Horário inválido. Os agendamentos devem ser alinhados a intervalos de " + slotDurationMinutes + " minutos.");
        }
    }
}
