package com.barbershop.security;

import com.barbershop.domain.Appointment;
import com.barbershop.domain.AppointmentStatus;
import com.barbershop.domain.Barber;
import com.barbershop.domain.BarberService;
import com.barbershop.dto.CancelAppointmentRequest;
import com.barbershop.dto.CreateAppointmentRequest;
import com.barbershop.dto.LoginRequest;
import com.barbershop.notification.AppointmentNotification;
import com.barbershop.notification.NotificationService;
import com.barbershop.repository.AppointmentRepository;
import com.barbershop.repository.BarberRepository;
import com.barbershop.repository.BarberServiceRepository;
import com.barbershop.repository.WorkScheduleRepository;
import com.barbershop.domain.WorkSchedule;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.timeout;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class SecurityAndAdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BarberRepository barberRepository;

    @Autowired
    private BarberServiceRepository serviceRepository;

    @Autowired
    private WorkScheduleRepository workScheduleRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private NotificationService notificationService;

    private final String BARBER_EMAIL = "barbeiro@example.invalid";
    private final String BARBER_PASS = "Senha-Ficticia-Teste-123";

    @BeforeEach
    void setUp() {
        appointmentRepository.deleteAll();
        serviceRepository.deleteAll();
        workScheduleRepository.deleteAll();
        barberRepository.deleteAll();

        // Cria o barbeiro de teste
        barberRepository.save(Barber.builder()
                .username("barbeiro")
                .displayName("Barbeiro Teste")
                .email(BARBER_EMAIL)
                .password(passwordEncoder.encode(BARBER_PASS))
                .build());
    }

    @Test
    @DisplayName("Login válido: deve autenticar e retornar token JWT com status 200")
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest(BARBER_EMAIL, BARBER_PASS);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value(BARBER_EMAIL));
    }

    @Test
    @DisplayName("Login inválido: senha incorreta deve retornar erro 401")
    void shouldFailLoginWithWrongPassword() throws Exception {
        LoginRequest request = new LoginRequest(BARBER_EMAIL, "SenhaErrada");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha incorretos."));
    }

    @Test
    @DisplayName("Admin sem token: deve ser rejeitado com HTTP 401 e ProblemDetail")
    void shouldRejectAdminAccessWithoutToken() throws Exception {
        mockMvc.perform(get("/api/admin/services"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Acesso não autorizado. Token ausente, inválido ou expirado."));
    }

    @Test
    @DisplayName("Saúde da aplicação é pública e outros endpoints Actuator não são expostos")
    void shouldExposeOnlyPublicHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());

        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin com token válido: deve permitir acesso com HTTP 200")
    void shouldAllowAdminAccessWithValidToken() throws Exception {
        String token = jwtService.generateToken(BARBER_EMAIL, "BARBER");

        mockMvc.perform(get("/api/admin/services")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Foto de serviço: barbeiro envia imagem e clientes recebem URL pública")
    void shouldUploadAndServeServicePhoto() throws Exception {
        String token = jwtService.generateToken(BARBER_EMAIL, "BARBER");
        BarberService service = serviceRepository.save(BarberService.builder()
                .name("Corte com Foto")
                .price(new BigDecimal("30.00"))
                .active(true)
                .build());
        MockMultipartFile photo = createPngPhoto();

        String photoUrl = mockMvc.perform(multipart("/api/admin/services/" + service.getId() + "/photo")
                        .file(photo)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String storedPhotoUrl = objectMapper.readTree(photoUrl).get("photoUrl").asText();

        mockMvc.perform(get("/api/public/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].photoUrl").value(storedPhotoUrl));
        mockMvc.perform(get(storedPhotoUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_PNG));
    }

    @Test
    @DisplayName("Foto de serviço: upload exige autenticação e valida formato")
    void shouldRequireAuthenticationAndRejectInvalidPhoto() throws Exception {
        BarberService service = serviceRepository.save(BarberService.builder()
                .name("Corte Sem Foto")
                .price(new BigDecimal("30.00"))
                .active(true)
                .build());
        MockMultipartFile invalidPhoto = new MockMultipartFile(
                "photo", "foto.jpg", MediaType.IMAGE_JPEG_VALUE, "not an image".getBytes());

        mockMvc.perform(multipart("/api/admin/services/" + service.getId() + "/photo")
                        .file(invalidPhoto))
                .andExpect(status().isUnauthorized());

        String token = jwtService.generateToken(BARBER_EMAIL, "BARBER");
        mockMvc.perform(multipart("/api/admin/services/" + service.getId() + "/photo")
                        .file(invalidPhoto)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("O arquivo enviado não é uma imagem JPG ou PNG válida."));
    }

    @Test
    @DisplayName("Foto de serviço: rejeita arquivo maior que 5 MB")
    void shouldRejectOversizedServicePhoto() throws Exception {
        String token = jwtService.generateToken(BARBER_EMAIL, "BARBER");
        BarberService service = serviceRepository.save(BarberService.builder()
                .name("Corte Foto Grande")
                .price(new BigDecimal("30.00"))
                .active(true)
                .build());
        MockMultipartFile oversizedPhoto = new MockMultipartFile(
                "photo", "foto.png", MediaType.IMAGE_PNG_VALUE, new byte[5 * 1024 * 1024 + 1]);

        mockMvc.perform(multipart("/api/admin/services/" + service.getId() + "/photo")
                        .file(oversizedPhoto)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("A foto deve ter no máximo 5 MB."));
    }

    private MockMultipartFile createPngPhoto() throws Exception {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return new MockMultipartFile("photo", "foto.png", MediaType.IMAGE_PNG_VALUE, output.toByteArray());
    }

    @Test
    @DisplayName("Cancelamento: cancelar agendamento deve liberar o horário no banco para novo agendamento")
    void shouldCancelAppointmentAndFreeTheSlot() throws Exception {
        String token = jwtService.generateToken(BARBER_EMAIL, "BARBER");

        // 1. Cadastra serviço e expediente
        BarberService service = serviceRepository.save(BarberService.builder()
                .name("Corte Teste")
                .price(new BigDecimal("30.00"))
                .active(true)
                .build());

        LocalDate appointmentDate = ZonedDateTime.now(ZoneId.of("America/Bahia")).toLocalDate().plusDays(2);
        LocalTime slotTime = LocalTime.of(14, 0);

        workScheduleRepository.save(WorkSchedule.builder()
                .dayOfWeek(appointmentDate.getDayOfWeek())
                .workDay(true)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(18, 0))
                .build());

        // 2. Cria o primeiro agendamento
        Appointment appointment = appointmentRepository.save(Appointment.builder()
                .clientName("Cliente 1")
                .clientPhone("00000000000")
                .service(service)
                .appointmentDate(appointmentDate)
                .startTime(slotTime)
                .status(AppointmentStatus.ACTIVE)
                .build());

        // 3. Cancela o agendamento via endpoint admin
        CancelAppointmentRequest cancelRequest = new CancelAppointmentRequest("Cliente cancelou");
        mockMvc.perform(post("/api/admin/appointments/" + appointment.getId() + "/cancel")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // 4. Cria NOVO agendamento exatamente no mesmo horário — deve ter sucesso pelo índice parcial
        CreateAppointmentRequest newBooking = new CreateAppointmentRequest(
                service.getId(), appointmentDate, slotTime, "Cliente 2", "00000000000"
        );

        mockMvc.perform(post("/api/public/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newBooking)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientName").value("Cliente 2"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // No banco: 1 cancelado + 1 ativo no mesmo horário
        assertEquals(2, appointmentRepository.count());
    }

    @Test
    @DisplayName("Agendamento confirmado: notifica após o commit")
    void shouldNotifyAfterAppointmentIsCommitted() throws Exception {
        BarberService service = serviceRepository.save(BarberService.builder()
                .name("Corte Teste")
                .price(new BigDecimal("30.00"))
                .active(true)
                .build());
        LocalDate appointmentDate = ZonedDateTime.now(ZoneId.of("America/Bahia")).toLocalDate().plusDays(2);
        workScheduleRepository.save(WorkSchedule.builder()
                .dayOfWeek(appointmentDate.getDayOfWeek())
                .workDay(true)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(18, 0))
                .build());

        CreateAppointmentRequest request = new CreateAppointmentRequest(
                service.getId(), appointmentDate, LocalTime.of(14, 0), "Cliente Teste", "00000000000");
        mockMvc.perform(post("/api/public/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(notificationService, timeout(5000)).notifyAppointmentCreated(any(AppointmentNotification.class));
        assertEquals(1, appointmentRepository.count());
    }

    @Test
    @DisplayName("Cancelamento: notifica após o commit")
    void shouldNotifyAfterAppointmentCancellationIsCommitted() throws Exception {
        String token = jwtService.generateToken(BARBER_EMAIL, "BARBER");
        BarberService service = serviceRepository.save(BarberService.builder()
                .name("Corte Teste")
                .price(new BigDecimal("30.00"))
                .active(true)
                .build());
        LocalDate appointmentDate = ZonedDateTime.now(ZoneId.of("America/Bahia")).toLocalDate().plusDays(2);
        Appointment appointment = appointmentRepository.save(Appointment.builder()
                .clientName("Cliente Teste")
                .clientPhone("00000000000")
                .service(service)
                .appointmentDate(appointmentDate)
                .startTime(LocalTime.of(14, 0))
                .status(AppointmentStatus.ACTIVE)
                .build());

        mockMvc.perform(post("/api/admin/appointments/" + appointment.getId() + "/cancel")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelAppointmentRequest("Solicitado pelo cliente"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(notificationService, timeout(5000)).notifyAppointmentCancelled(any(AppointmentNotification.class));
    }

    @Test
    @DisplayName("Falha na notificação: não impede a confirmação do agendamento")
    void shouldKeepAppointmentSuccessfulWhenNotificationFails() throws Exception {
        doThrow(new IllegalStateException("Falha simulada no envio"))
                .when(notificationService).notifyAppointmentCreated(any(AppointmentNotification.class));
        BarberService service = serviceRepository.save(BarberService.builder()
                .name("Corte Teste")
                .price(new BigDecimal("30.00"))
                .active(true)
                .build());
        LocalDate appointmentDate = ZonedDateTime.now(ZoneId.of("America/Bahia")).toLocalDate().plusDays(2);
        workScheduleRepository.save(WorkSchedule.builder()
                .dayOfWeek(appointmentDate.getDayOfWeek())
                .workDay(true)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(18, 0))
                .build());
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                service.getId(), appointmentDate, LocalTime.of(14, 0), "Cliente Teste", "00000000000");

        mockMvc.perform(post("/api/public/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(notificationService, timeout(5000)).notifyAppointmentCreated(any(AppointmentNotification.class));
        assertEquals(1, appointmentRepository.count());
    }

    @Test
    @DisplayName("Falha na notificação de cancelamento: não desfaz o cancelamento")
    void shouldKeepCancellationSuccessfulWhenNotificationFails() throws Exception {
        doThrow(new IllegalStateException("Falha simulada no envio"))
                .when(notificationService).notifyAppointmentCancelled(any(AppointmentNotification.class));
        String token = jwtService.generateToken(BARBER_EMAIL, "BARBER");
        BarberService service = serviceRepository.save(BarberService.builder()
                .name("Corte Teste")
                .price(new BigDecimal("30.00"))
                .active(true)
                .build());
        LocalDate appointmentDate = ZonedDateTime.now(ZoneId.of("America/Bahia")).toLocalDate().plusDays(2);
        Appointment appointment = appointmentRepository.save(Appointment.builder()
                .clientName("Cliente Teste")
                .clientPhone("00000000000")
                .service(service)
                .appointmentDate(appointmentDate)
                .startTime(LocalTime.of(14, 0))
                .status(AppointmentStatus.ACTIVE)
                .build());

        mockMvc.perform(post("/api/admin/appointments/" + appointment.getId() + "/cancel")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelAppointmentRequest("Solicitado pelo cliente"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(notificationService, timeout(5000)).notifyAppointmentCancelled(any(AppointmentNotification.class));
        assertEquals(AppointmentStatus.CANCELLED,
                appointmentRepository.findById(appointment.getId()).orElseThrow().getStatus());
    }
}
