package com.barbershop.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Expediente semanal; folgas usam `workDay = false` e horários nulos.
 */
@Entity
@Table(
    name = "work_schedules",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_work_schedule_day",
        columnNames = {"day_of_week"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "break_start")
    private LocalTime breakStart;

    @Column(name = "break_end")
    private LocalTime breakEnd;

    @Column(name = "work_day", nullable = false)
    @Builder.Default
    private boolean workDay = true;
}
