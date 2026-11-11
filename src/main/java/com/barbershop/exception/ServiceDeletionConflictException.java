package com.barbershop.exception;

public class ServiceDeletionConflictException extends RuntimeException {
    public ServiceDeletionConflictException(String message) {
        super(message);
    }
}
