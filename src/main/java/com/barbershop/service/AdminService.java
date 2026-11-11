package com.barbershop.service;

import com.barbershop.domain.*;
import com.barbershop.dto.*;
import com.barbershop.exception.BusinessException;
import com.barbershop.exception.ResourceNotFoundException;
import com.barbershop.exception.ServiceDeletionConflictException;
import com.barbershop.notification.AppointmentCancelledEvent;
import com.barbershop.notification.AppointmentNotification;
import com.barbershop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final BarberServiceRepository serviceRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final BlockedSlotRepository blockedSlotRepository;
    private final AppointmentRepository appointmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    // --- CRUD Serviços ---
    public List<BarberService> listAllServices() {
        return serviceRepository.findAll();
    }

    @Transactional
    public BarberService createService(ServiceRequest request) {
        if (serviceRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new BusinessException("Já existe um serviço com esse nome.");
        }
        BarberService service = BarberService.builder()
                .name(request.name().trim())
                .description(request.description())
                .price(request.price())
                .active(true)
                .build();
        return serviceRepository.save(service);
    }

    @Transactional
    public BarberService updateService(Long id, ServiceRequest request) {
        BarberService service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado."));

        if (serviceRepository.existsByNameIgnoreCaseAndIdNot(request.name().trim(), id)) {
            throw new BusinessException("Já existe outro serviço com esse nome.");
        }

        service.setName(request.name().trim());
        service.setDescription(request.description());
        service.setPrice(request.price());
        if (request.active() != null) {
            service.setActive(request.active());
        }
        return serviceRepository.save(service);
    }

    @Transactional
    public void deleteService(Long id) {
        BarberService service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado."));
        if (service.isActive()) {
            throw new ServiceDeletionConflictException("Desative o serviço antes de apagá-lo.");
        }
        if (appointmentRepository.existsByService_Id(id)) {
            throw new ServiceDeletionConflictException(
                    "Este serviço já foi usado em agendamentos e não pode ser apagado. Ele continua desativado.");
        }
        serviceRepository.delete(service);
    }

    // --- WorkSchedule ---
    public List<WorkSchedule> listSchedules() {
        return workScheduleRepository.findAll();
    }

    @Transactional
    public WorkSchedule saveSchedule(WorkScheduleRequest request) {
        WorkSchedule schedule = workScheduleRepository.findByDayOfWeek(request.dayOfWeek())
                .orElseGet(() -> WorkSchedule.builder().dayOfWeek(request.dayOfWeek()).build());

        schedule.setWorkDay(request.workDay());
        if (request.workDay()) {
            if (request.startTime() == null || request.endTime() == null || !request.endTime().isAfter(request.startTime())) {
                throw new BusinessException("Horário de início e fim obrigatórios, e fim deve ser maior que início.");
            }
            if (request.breakStart() != null && request.breakEnd() != null) {
                if (!request.breakEnd().isAfter(request.breakStart())) {
                    throw new BusinessException("Fim do intervalo deve ser após o início.");
                }
            }
            schedule.setStartTime(request.startTime());
            schedule.setEndTime(request.endTime());
            schedule.setBreakStart(request.breakStart());
            schedule.setBreakEnd(request.breakEnd());
        } else {
            schedule.setStartTime(null);
            schedule.setEndTime(null);
            schedule.setBreakStart(null);
            schedule.setBreakEnd(null);
        }
        return workScheduleRepository.save(schedule);
    }

    // --- BlockedSlot ---
    public List<BlockedSlot> listBlockedSlots() {
        return blockedSlotRepository.findAll();
    }

    @Transactional
    public BlockedSlot createBlockedSlot(BlockedSlotRequest request) {
        if (Boolean.FALSE.equals(request.fullDay())) {
            if (request.startTime() == null || request.endTime() == null || !request.endTime().isAfter(request.startTime())) {
                throw new BusinessException("Para bloqueios parciais, informe início e fim válidos.");
            }
        }
        BlockedSlot block = BlockedSlot.builder()
                .date(request.date())
                .fullDay(request.fullDay())
                .startTime(request.fullDay() ? null : request.startTime())
                .endTime(request.fullDay() ? null : request.endTime())
                .reason(request.reason())
                .build();
        return blockedSlotRepository.save(block);
    }

    @Transactional
    public void deleteBlockedSlot(Long id) {
        blockedSlotRepository.deleteById(id);
    }

    // --- Agenda do Dia e da Semana ---
    public List<AppointmentResponse> getDailyAppointments(LocalDate date) {
        return appointmentRepository.findByAppointmentDateAndStatusOrderByStartTimeAsc(date, AppointmentStatus.ACTIVE)
                .stream()
                .map(AppointmentResponse::from)
                .toList();
    }

    public List<AppointmentResponse> getWeeklyAppointments(LocalDate fromDate) {
        LocalDate toDate = fromDate.plusDays(6);
        return appointmentRepository.findByAppointmentDateBetweenAndStatusOrderByAppointmentDateAscStartTimeAsc(
                        fromDate, toDate, AppointmentStatus.ACTIVE)
                .stream()
                .map(AppointmentResponse::from)
                .toList();
    }

    // --- Cancelamento liberando o horário ---
    @Transactional
    public AppointmentResponse cancelAppointment(Long appointmentId, CancelAppointmentRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException("Agendamento não encontrado."));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessException("Este agendamento já se encontra cancelado.");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(request.reason().trim());
        Appointment saved = appointmentRepository.save(appointment);
        eventPublisher.publishEvent(new AppointmentCancelledEvent(
                AppointmentNotification.from(saved, saved.getCancellationReason())));

        return AppointmentResponse.from(saved);
    }
}