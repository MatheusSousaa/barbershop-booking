package com.barbershop.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Exceção de negócio — retorna 422 por padrão. */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
