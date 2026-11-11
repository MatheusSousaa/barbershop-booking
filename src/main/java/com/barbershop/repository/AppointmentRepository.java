package com.barbershop.repository;

import com.barbershop.domain.Appointment;
import com.barbershop.domain.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByAppointmentDateAndStatusOrderByStartTimeAsc(
            LocalDate date, AppointmentStatus status);

    List<Appointment> findByAppointmentDateBetweenAndStatusOrderByAppointmentDateAscStartTimeAsc(
            LocalDate from, LocalDate to, AppointmentStatus status);

    /**
     * Checagem preventiva; o índice único parcial é a garantia final
     * contra reservas simultâneas do mesmo horário.
     */
    boolean existsByAppointmentDateAndStartTimeAndStatus(
            LocalDate date, LocalTime startTime, AppointmentStatus status);
}
