package com.barbershop.config;

import com.barbershop.domain.Barber;
import com.barbershop.repository.BarberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Cria o barbeiro inicial a partir das credenciais configuradas no ambiente. */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final BarberRepository barberRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${barbershop.initial-barber.username:}")
    private String initialUsername;

    @Value("${barbershop.initial-barber.email:}")
    private String initialEmail;

    @Value("${barbershop.initial-barber.password:}")
    private String initialPassword;

    @Value("${barbershop.initial-barber.display-name:Barbeiro}")
    private String initialDisplayName;

    @Value("${barbershop.initial-barber.force-password-reset:false}")
    private boolean forcePasswordReset;

    @Override
    public void run(String... args) {
        if (initialEmail == null || initialEmail.isBlank() || initialPassword == null || initialPassword.isBlank()) {
            log.info("Nenhuma credencial inicial de barbeiro fornecida via variáveis de ambiente.");
            return;
        }

        if (barberRepository.count() == 0) {
            String username = (initialUsername != null && !initialUsername.isBlank())
                    ? initialUsername
                    : initialEmail.split("@")[0];

            Barber barber = Barber.builder()
                    .username(username)
                    .email(initialEmail.trim().toLowerCase())
                    .password(passwordEncoder.encode(initialPassword))
                    .displayName(initialDisplayName)
                    .build();

            barberRepository.save(barber);
            log.info("Conta inicial de barbeiro criada com sucesso.");
        } else if (forcePasswordReset) {
            barberRepository.findByEmail(initialEmail.trim().toLowerCase()).ifPresentOrElse(barber -> {
                barber.setPassword(passwordEncoder.encode(initialPassword));
                barberRepository.save(barber);
                log.info("Senha da conta inicial de barbeiro redefinida por configuração explícita.");
            }, () -> log.warn("A redefinição de senha foi solicitada, mas não há conta com o e-mail inicial configurado."));
        } else {
            log.info("Barbeiro já existe no banco de dados. Nenhuma ação necessária.");
        }
    }
}
