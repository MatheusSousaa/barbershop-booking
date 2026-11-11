package com.barbershop.service;

import com.barbershop.domain.Appointment;
import com.barbershop.domain.AppointmentStatus;
import com.barbershop.domain.BlockedSlot;
import com.barbershop.domain.WorkSchedule;
import com.barbershop.exception.BusinessException;
import com.barbershop.repository.AppointmentRepository;
import com.barbershop.repository.BlockedSlotRepository;
import com.barbershop.repository.WorkScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlotCalculatorServiceTest {

    @Mock
    private WorkScheduleRepository workScheduleRepository;

    @Mock
    private BlockedSlotRepository blockedSlotRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private SlotCalculatorService slotCalculatorService;

    private final String TIMEZONE = "America/Bahia";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(slotCalculatorService, "slotDurationMinutes", 30);
        ReflectionTestUtils.setField(slotCalculatorService, "timezone", TIMEZONE);
    }

    @Test
    @DisplayName("Hoje: slots que já passaram do horário atual no fuso America/Bahia devem sumir")
    void shouldFilterOutPastSlotsForToday() {
        ZoneId zone = ZoneId.of(TIMEZONE);
        ZonedDateTime now = ZonedDateTime.now(zone);
        LocalDate today = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();

        // Expediente cobrindo o dia todo
        WorkSchedule schedule = WorkSchedule.builder()
                .dayOfWeek(today.getDayOfWeek())
                .workDay(true)
                .startTime(LocalTime.of(0, 0))
                .endTime(LocalTime.of(23, 59))
                .build();
        when(workScheduleRepository.findByDayOfWeek(today.getDayOfWeek())).thenReturn(Optional.of(schedule));
        when(blockedSlotRepository.findByDate(today)).thenReturn(List.of());
        when(appointmentRepository.findByAppointmentDateAndStatusOrderByStartTimeAsc(today, AppointmentStatus.ACTIVE))
                .thenReturn(List.of());

        List<LocalTime> slots = slotCalculatorService.getAvailableSlots(today);

        // Nenhum slot retornado pode ser menor ou igual a currentTime
        for (LocalTime slot : slots) {
            assertTrue(slot.isAfter(currentTime), "Slot " + slot + " não deveria ser anterior/igual ao horário atual " + currentTime);
        }
    }

    @Test
    @DisplayName("Folga semanal (workDay = false): não deve gerar nenhum horário")
    void shouldReturnEmptyWhenDayOff() {
        LocalDate date = ZonedDateTime.now(ZoneId.of(TIMEZONE)).toLocalDate().plusDays(2);
        WorkSchedule schedule = WorkSchedule.builder()
                .dayOfWeek(date.getDayOfWeek())
                .workDay(false)
                .build();
        when(workScheduleRepository.findByDayOfWeek(date.getDayOfWeek())).thenReturn(Optional.of(schedule));

        List<LocalTime> slots = slotCalculatorService.getAvailableSlots(date);
        assertTrue(slots.isEmpty());
    }

    @Test
    @DisplayName("Dia sem WorkSchedule configurado: não deve gerar nenhum horário")
    void shouldReturnEmptyWhenNoScheduleExists() {
        LocalDate date = ZonedDateTime.now(ZoneId.of(TIMEZONE)).toLocalDate().plusDays(2);
        when(workScheduleRepository.findByDayOfWeek(date.getDayOfWeek())).thenReturn(Optional.empty());

        List<LocalTime> slots = slotCalculatorService.getAvailableSlots(date);
        assertTrue(slots.isEmpty());
    }

    @Test
    @DisplayName("Bloqueio de dia inteiro: não deve gerar nenhum horário")
    void shouldReturnEmptyWhenFullDayBlocked() {
        LocalDate date = ZonedDateTime.now(ZoneId.of(TIMEZONE)).toLocalDate().plusDays(2);
        WorkSchedule schedule = WorkSchedule.builder()
                .dayOfWeek(date.getDayOfWeek())
                .workDay(true)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(18, 0))
                .build();
        when(workScheduleRepository.findByDayOfWeek(date.getDayOfWeek())).thenReturn(Optional.of(schedule));

        BlockedSlot fullBlock = BlockedSlot.builder()
                .date(date)
                .fullDay(true)
                .build();
        when(blockedSlotRepository.findByDate(date)).thenReturn(List.of(fullBlock));

        List<LocalTime> slots = slotCalculatorService.getAvailableSlots(date);
        assertTrue(slots.isEmpty());
    }

    @Test
    @DisplayName("Intervalo de almoço, bloqueio parcial e horário já ocupado devem ser subtraídos")
    void shouldFilterBreakPartialBlockAndBookedSlots() {
        LocalDate date = ZonedDateTime.now(ZoneId.of(TIMEZONE)).toLocalDate().plusDays(2);
        DayOfWeek dow = date.getDayOfWeek();

        // Expediente: 08:00 às 12:00, almoço 10:00 às 11:00
        WorkSchedule schedule = WorkSchedule.builder()
                .dayOfWeek(dow)
                .workDay(true)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(12, 0))
                .breakStart(LocalTime.of(10, 0))
                .breakEnd(LocalTime.of(11, 0))
                .build();
        when(workScheduleRepository.findByDayOfWeek(dow)).thenReturn(Optional.of(schedule));

        // Bloqueio parcial: 09:00 às 09:30
        BlockedSlot partialBlock = BlockedSlot.builder()
                .date(date)
                .fullDay(false)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(9, 30))
                .build();
        when(blockedSlotRepository.findByDate(date)).thenReturn(List.of(partialBlock));

        // Agendamento ativo: 11:30
        Appointment booked = Appointment.builder()
                .startTime(LocalTime.of(11, 30))
                .status(AppointmentStatus.ACTIVE)
                .build();
        when(appointmentRepository.findByAppointmentDateAndStatusOrderByStartTimeAsc(eq(date), eq(AppointmentStatus.ACTIVE)))
                .thenReturn(List.of(booked));

        // Slots totais possíveis (30m):
        // 08:00 -> LIVRE
        // 08:30 -> LIVRE
        // 09:00 -> BLOQUEIO PARCIAL
        // 09:30 -> LIVRE
        // 10:00 -> ALMOÇO
        // 10:30 -> ALMOÇO
        // 11:00 -> LIVRE
        // 11:30 -> AGENDADO
        List<LocalTime> slots = slotCalculatorService.getAvailableSlots(date);

        assertEquals(List.of(
                LocalTime.of(8, 0),
                LocalTime.of(8, 30),
                LocalTime.of(9, 30),
                LocalTime.of(11, 0)
        ), slots);
    }

    @Test
    @DisplayName("Data no passado deve lançar BusinessException")
    void shouldThrowExceptionForPastDate() {
        LocalDate pastDate = ZonedDateTime.now(ZoneId.of(TIMEZONE)).toLocalDate().minusDays(1);
        assertThrows(BusinessException.class, () -> slotCalculatorService.getAvailableSlots(pastDate));
    }
}
