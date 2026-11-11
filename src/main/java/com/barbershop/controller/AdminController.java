package com.barbershop.controller;

import com.barbershop.domain.BarberService;
import com.barbershop.domain.BlockedSlot;
import com.barbershop.domain.WorkSchedule;
import com.barbershop.dto.*;
import com.barbershop.service.AdminService;
import com.barbershop.service.ServicePhotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Administração", description = "Painel exclusivo do barbeiro autenticado")
public class AdminController {

    private final AdminService adminService;
    private final ServicePhotoService servicePhotoService;

    // --- Serviços ---
    @GetMapping("/services")
    @Operation(summary = "Lista todos os serviços (incluindo inativos)")
    public ResponseEntity<List<BarberService>> listServices() {
        return ResponseEntity.ok(adminService.listAllServices());
    }

    @PostMapping("/services")
    @Operation(summary = "Cadastra um novo serviço")
    public ResponseEntity<BarberService> createService(@Valid @RequestBody ServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createService(request));
    }

    @PutMapping("/services/{id}")
    @Operation(summary = "Edita um serviço existente")
    public ResponseEntity<BarberService> updateService(@PathVariable Long id, @Valid @RequestBody ServiceRequest request) {
        return ResponseEntity.ok(adminService.updateService(id, request));
    }

    @DeleteMapping("/services/{id}")
    @Operation(summary = "Apaga um serviço desativado que nunca foi usado em agendamentos")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        adminService.deleteService(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(path = "/services/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Adiciona ou substitui a foto de um serviço")
    public ResponseEntity<BarberService> uploadServicePhoto(
            @PathVariable Long id,
            @RequestPart("photo") MultipartFile photo) {
        return ResponseEntity.ok(servicePhotoService.upload(id, photo));
    }

    // --- WorkSchedule ---
    @GetMapping("/schedules")
    @Operation(summary = "Lista configurações de expediente semanal")
    public ResponseEntity<List<WorkSchedule>> listSchedules() {
        return ResponseEntity.ok(adminService.listSchedules());
    }

    @PutMapping("/schedules")
    @Operation(summary = "Define ou atualiza o expediente de um dia da semana")
    public ResponseEntity<WorkSchedule> saveSchedule(@Valid @RequestBody WorkScheduleRequest request) {
        return ResponseEntity.ok(adminService.saveSchedule(request));
    }

    // --- BlockedSlot ---
    @GetMapping("/blocked-slots")
    @Operation(summary = "Lista todos os bloqueios pontuais")
    public ResponseEntity<List<BlockedSlot>> listBlockedSlots() {
        return ResponseEntity.ok(adminService.listBlockedSlots());
    }

    @PostMapping("/blocked-slots")
    @Operation(summary = "Cria um bloqueio pontual de agenda")
    public ResponseEntity<BlockedSlot> createBlockedSlot(@Valid @RequestBody BlockedSlotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createBlockedSlot(request));
    }

    @DeleteMapping("/blocked-slots/{id}")
    @Operation(summary = "Remove um bloqueio pontual")
    public ResponseEntity<Void> deleteBlockedSlot(@PathVariable Long id) {
        adminService.deleteBlockedSlot(id);
        return ResponseEntity.noContent().build();
    }

    // --- Agenda ---
    @GetMapping("/appointments/daily")
    @Operation(summary = "Visualiza a agenda do dia")
    public ResponseEntity<List<AppointmentResponse>> getDailyAppointments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(adminService.getDailyAppointments(date));
    }

    @GetMapping("/appointments/weekly")
    @Operation(summary = "Visualiza a agenda da semana (a partir de uma data)")
    public ResponseEntity<List<AppointmentResponse>> getWeeklyAppointments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate) {
        return ResponseEntity.ok(adminService.getWeeklyAppointments(fromDate));
    }

    // --- Cancelamento ---
    @PostMapping("/appointments/{id}/cancel")
    @Operation(summary = "Cancela um agendamento com motivo e libera o horário no banco")
    public ResponseEntity<AppointmentResponse> cancelAppointment(
            @PathVariable Long id,
            @Valid @RequestBody CancelAppointmentRequest request) {
        return ResponseEntity.ok(adminService.cancelAppointment(id, request));
    }
}
