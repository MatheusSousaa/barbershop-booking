package com.barbershop.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * O índice único parcial de agendamentos ativos é gerenciado pelo Flyway,
 * pois uma restrição JPA também impediria reutilizar horários cancelados.
 */
@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "client_name", nullable = false, length = 100)
    private String clientName;

    @NotBlank
    @Size(max = 20)
    @Column(name = "client_phone", nullable = false, length = 20)
    private String clientPhone;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private BarberService service;

    @NotNull
    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    /** Guarda apenas o início; a duração é definida globalmente nesta versão. */
    @NotNull
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.ACTIVE;

    @Size(max = 255)
    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;
}
