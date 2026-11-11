package com.barbershop.service;

import com.barbershop.domain.Barber;
import com.barbershop.dto.LoginRequest;
import com.barbershop.dto.LoginResponse;
import com.barbershop.exception.BusinessException;
import com.barbershop.repository.BarberRepository;
import com.barbershop.security.JwtService;
import com.barbershop.security.RateLimitingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final BarberRepository barberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RateLimitingService rateLimitingService;

    @Value("${barbershop.jwt.expiration-ms}")
    private long expirationMs;

    public LoginResponse login(LoginRequest request, String clientIp) {
        if (!rateLimitingService.tryConsumeLogin(clientIp)) {
            throw new BusinessException("Muitas tentativas de login. Aguarde um momento e tente novamente.");
        }

        Barber barber = barberRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new com.barbershop.exception.UnauthorizedException("E-mail ou senha incorretos."));

        if (!passwordEncoder.matches(request.password(), barber.getPassword())) {
            throw new com.barbershop.exception.UnauthorizedException("E-mail ou senha incorretos.");
        }

        String token = jwtService.generateToken(barber.getEmail(), "BARBER");

        return new LoginResponse(
                token,
                "Bearer",
                expirationMs,
                barber.getDisplayName(),
                barber.getEmail()
        );
    }
}
