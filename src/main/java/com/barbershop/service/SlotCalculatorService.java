package com.barbershop.service;

import com.barbershop.domain.AppointmentStatus;
import com.barbershop.domain.BlockedSlot;
import com.barbershop.domain.WorkSchedule;
import com.barbershop.exception.BusinessException;
import com.barbershop.repository.AppointmentRepository;
import com.barbershop.repository.BlockedSlotRepository;
import com.barbershop.repository.WorkScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Serviço responsável pelo cálculo dinâmico de horários livres.
 */
@Service
@RequiredArgsConstructor
public class SlotCalculatorService {

    private final WorkScheduleRepository workScheduleRepository;
    private final BlockedSlotRepository blockedSlotRepository;
    private final AppointmentRepository appointmentRepository;

    @Value("${barbershop.slot.duration-minutes}")
    private int slotDurationMinutes;

    @Value("${barbershop.timezone:America/Bahia}")
    private String timezone;

    /**
     * Calcula horários disponíveis para uma data específica.
     */
    public List<LocalTime> getAvailableSlots(LocalDate date) {
        ZoneId zoneId = ZoneId.of(timezone);
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        LocalDate today = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();

        if (date.isBefore(today)) {
            throw new BusinessException("Não é permitido agendar em datas no passado.");
        }

        WorkSchedule schedule = workScheduleRepository.findByDayOfWeek(date.getDayOfWeek())
                .orElse(null);

        // Se não houver configuração ou for folga
        if (schedule == null || !schedule.isWorkDay()) {
            return List.of();
        }

        // Bloqueios pontuais do dia
        List<BlockedSlot> blocks = blockedSlotRepository.findByDate(date);
        if (blocks.stream().anyMatch(BlockedSlot::isFullDay)) {
            return List.of();
        }

        // Horários já agendados e ativos
        Set<LocalTime> booked = appointmentRepository
                .findByAppointmentDateAndStatusOrderByStartTimeAsc(date, AppointmentStatus.ACTIVE)
                .stream()
                .map(a -> a.getStartTime())
                .collect(Collectors.toSet());

        List<LocalTime> availableSlots = new ArrayList<>();
        LocalTime cursor = schedule.getStartTime();
        LocalTime end = schedule.getEndTime();

        while (true) {
            LocalTime next = cursor.plusMinutes(slotDurationMinutes);
            // Se virar o dia ou ultrapassar o fim do expediente
            if (next.isBefore(cursor) || next.isAfter(end)) {
                break;
            }

            // Se for hoje, não permite horários passados
            boolean isPastToday = date.isEqual(today) && !cursor.isAfter(currentTime);

            if (!isPastToday && !booked.contains(cursor) && !isBlocked(cursor, blocks, schedule)) {
                availableSlots.add(cursor);
            }
            cursor = next;
        }

        return availableSlots;
    }

    private boolean isBlocked(LocalTime slot, List<BlockedSlot> blocks, WorkSchedule schedule) {
        // Intervalo/almoço
        if (schedule.getBreakStart() != null && schedule.getBreakEnd() != null) {
            if (!slot.isBefore(schedule.getBreakStart()) && slot.isBefore(schedule.getBreakEnd())) {
                return true;
            }
        }

        // Bloqueios pontuais parciais
        for (BlockedSlot b : blocks) {
            if (!b.isFullDay() && b.getStartTime() != null && b.getEndTime() != null) {
                if (!slot.isBefore(b.getStartTime()) && slot.isBefore(b.getEndTime())) {
                    return true;
                }
            }
        }
        return false;
    }
}
