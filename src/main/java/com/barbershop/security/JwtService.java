package com.barbershop.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utilitário para geração e validação de tokens JWT.
 */
@Component
public class JwtService {

    @Value("${barbershop.jwt.secret}")
    private String secret;

    @Value("${barbershop.jwt.expiration-ms}")
    private long expirationMs;

    @jakarta.annotation.PostConstruct
    public void validateSecret() {
        if (secret == null || secret.trim().length() < 32) {
            throw new IllegalStateException("JWT_SECRET é obrigatório e deve possuir no mínimo 32 caracteres.");
        }
    }

    private SecretKey getSigningKey() {
        if (secret == null || secret.trim().length() < 32) {
            throw new IllegalStateException("JWT_SECRET é obrigatório e deve possuir no mínimo 32 caracteres.");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String email, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
