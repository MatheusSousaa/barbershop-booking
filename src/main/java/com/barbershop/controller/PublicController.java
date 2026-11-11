package com.barbershop.controller;

import com.barbershop.dto.AvailableSlotsResponse;
import com.barbershop.dto.AppointmentResponse;
import com.barbershop.dto.CreateAppointmentRequest;
import com.barbershop.dto.ServiceResponse;
import com.barbershop.exception.BusinessException;
import com.barbershop.security.RateLimitingService;
import com.barbershop.service.AppointmentService;
import com.barbershop.service.ServiceCatalogService;
import com.barbershop.service.SlotCalculatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "Público", description = "Endpoints acessíveis aos clientes sem login")
public class PublicController {

    private final ServiceCatalogService serviceCatalogService;
    private final SlotCalculatorService slotCalculatorService;
    private final AppointmentService appointmentService;
    private final RateLimitingService rateLimitingService;

    @PostMapping("/appointments")
    @Operation(summary = "Realiza um novo agendamento público")
    public ResponseEntity<AppointmentResponse> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request,
            HttpServletRequest httpRequest) {

        String ip = getClientIp(httpRequest);
        if (!rateLimitingService.tryConsumeAppointment(ip)) {
            throw new BusinessException("Muitas solicitações de agendamento. Aguarde antes de tentar novamente.");
        }

        AppointmentResponse response = appointmentService.createAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private String getClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    @GetMapping("/services")
    @Operation(summary = "Lista serviços ativos disponíveis")
    public ResponseEntity<List<ServiceResponse>> listServices() {
        List<ServiceResponse> services = serviceCatalogService.listActive().stream()
                .map(service -> new ServiceResponse(service.getId(), service.getName(), service.getDescription(),
                        service.getPrice(), service.getPhotoUrl()))
                .toList();
        return ResponseEntity.ok(services);
    }

    @GetMapping("/slots")
    @Operation(summary = "Consulta horários livres para uma data específica")
    public ResponseEntity<AvailableSlotsResponse> getAvailableSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<LocalTime> slots = slotCalculatorService.getAvailableSlots(date);
        return ResponseEntity.ok(new AvailableSlotsResponse(date, slots));
    }
}
