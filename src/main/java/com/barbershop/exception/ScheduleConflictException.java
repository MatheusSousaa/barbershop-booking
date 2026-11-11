package com.barbershop.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Exceção lançada quando ocorre conflito de agendamento (retorna HTTP 409). */
@ResponseStatus(HttpStatus.CONFLICT)
public class ScheduleConflictException extends RuntimeException {
    public ScheduleConflictException(String message) {
        super(message);
    }
}
