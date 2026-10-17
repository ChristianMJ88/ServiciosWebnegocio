package com.techprotech.agenda.modulos.citas.aplicacion;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class AnticipoNoDisponibleException extends ResponseStatusException {
    public static final String MENSAJE = "Este servicio requiere un anticipo en línea que el negocio no tiene disponible en este momento. Contacta al negocio para reservar.";
    public AnticipoNoDisponibleException() { super(HttpStatus.CONFLICT, MENSAJE); }
}
