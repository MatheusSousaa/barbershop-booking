package com.barbershop.service;

import com.barbershop.domain.Appointment;
import com.barbershop.domain.AppointmentStatus;
import com.barbershop.domain.BarberService;
import com.barbershop.domain.WorkSchedule;
import com.barbershop.dto.CreateAppointmentRequest;
import com.barbershop.exception.ScheduleConflictException;
import com.barbershop.repository.AppointmentRepository;
import com.barbershop.repository.BarberServiceRepository;
import com.barbershop.repository.WorkScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class AppointmentConcurrencyIntegrationTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private BarberServiceRepository barberServiceRepository;

    @Autowired
    private WorkScheduleRepository workScheduleRepository;

    private Long serviceId;
    private LocalDate appointmentDate;
    private final LocalTime slotTime = LocalTime.of(10, 0);

    @BeforeEach
    void setUp() {
        appointmentRepository.deleteAll();
        workScheduleRepository.deleteAll();
        barberServiceRepository.deleteAll();

        // 1. Cria serviço ativo
        BarberService service = barberServiceRepository.save(BarberService.builder()
                .name("Corte Teste")
                .price(new BigDecimal("35.00"))
                .active(true)
                .build());
        serviceId = service.getId();

        // 2. Data de teste
        appointmentDate = ZonedDateTime.now(ZoneId.of("America/Bahia")).toLocalDate().plusDays(3);

        // 3. Cadastra expediente para o dia da semana
        workScheduleRepository.save(WorkSchedule.builder()
                .dayOfWeek(appointmentDate.getDayOfWeek())
                .workDay(true)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(18, 0))
                .build());
    }

    @Test
    @DisplayName("Concorrência: duas threads agendando exatamente o mesmo horário, apenas UMA deve vencer")
    void shouldAllowOnlyOneAppointmentUnderConcurrentRequests() throws InterruptedException {
        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        CreateAppointmentRequest request1 = new CreateAppointmentRequest(
                serviceId, appointmentDate, slotTime, "Cliente A", "00000000000"
        );

        CreateAppointmentRequest request2 = new CreateAppointmentRequest(
                serviceId, appointmentDate, slotTime, "Cliente B", "00000000000"
        );

        Runnable task1 = () -> {
            try {
                startLatch.await();
                appointmentService.createAppointment(request1);
                successCount.incrementAndGet();
            } catch (ScheduleConflictException e) {
                conflictCount.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        };

        Runnable task2 = () -> {
            try {
                startLatch.await();
                appointmentService.createAppointment(request2);
                successCount.incrementAndGet();
            } catch (ScheduleConflictException e) {
                conflictCount.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        };

        executorService.submit(task1);
        executorService.submit(task2);

        // Dispara as duas threads no mesmo instante
        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // Validações
        assertEquals(1, successCount.get(), "Exatamente um agendamento deveria ter sucesso.");
        assertEquals(1, conflictCount.get(), "Exatamente um agendamento deveria falhar por conflito.");

        // Confere registros no banco de dados PostgreSQL
        long activeCount = appointmentRepository.count();
        assertEquals(1, activeCount, "Deveria haver apenas 1 registro no banco de dados.");
    }
}
