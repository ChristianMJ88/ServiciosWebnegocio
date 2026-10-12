package com.techprotech.agenda.compartido.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class SesionNoDisponibleException extends ResponseStatusException {
    public SesionNoDisponibleException(String mensaje) { super(HttpStatus.UNAUTHORIZED, mensaje); }
}
