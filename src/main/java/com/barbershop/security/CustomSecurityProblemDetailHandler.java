package com.barbershop.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Trata respostas 401 (não autenticado) e 403 (não autorizado) no formato RFC 9457 ProblemDetail.
 */
@Component
public class CustomSecurityProblemDetailHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        writeProblemDetail(response, HttpStatus.UNAUTHORIZED, "Acesso não autorizado. Token ausente, inválido ou expirado.");
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        writeProblemDetail(response, HttpStatus.FORBIDDEN, "Você não tem permissão para acessar este recurso.");
    }

    private void writeProblemDetail(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }
}
