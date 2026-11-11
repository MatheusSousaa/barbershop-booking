package com.barbershop.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/** Bloqueio pontual de dia inteiro ou intervalo, independente do expediente semanal. */
@Entity
@Table(name = "blocked_slots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "full_day", nullable = false)
    @Builder.Default
    private boolean fullDay = false;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Size(max = 255)
    @Column(length = 255)
    private String reason;
}
