package com.barbershop.repository;

import com.barbershop.domain.WorkSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.Optional;

/**
 * Repositório do expediente semanal.
 */
@Repository
public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, Long> {

    /**
     * Busca a configuração de expediente para um dia da semana específico.
     * Retorna Optional pois um dia pode não ter configuração (ex.: domingo
     * sem registro = sem atendimento).
     */
    Optional<WorkSchedule> findByDayOfWeek(DayOfWeek dayOfWeek);
}
