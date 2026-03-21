package com.techprotech.agenda.compartido.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errores.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        log.warn("Error de validacion en request: {}", errores);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("codigo", "VALIDACION_INVALIDA");
        respuesta.put("mensaje", "La solicitud contiene errores de validacion");
        respuesta.put("errores", errores);
        respuesta.put("fechaHora", OffsetDateTime.now());

        return ResponseEntity.badRequest().body(respuesta);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorApi> manejarConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(new ErrorApi(
                "VALIDACION_INVALIDA",
                ex.getMessage(),
                OffsetDateTime.now()
        ));
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ErrorApi> manejarPendiente(UnsupportedOperationException ex) {
        log.warn("Funcionalidad pendiente invocada: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(new ErrorApi(
                "FUNCIONALIDAD_PENDIENTE",
                ex.getMessage(),
                OffsetDateTime.now()
        ));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorApi> manejarResponseStatus(ResponseStatusException ex) {
        log.warn("Error de negocio [{}]: {}", ex.getStatusCode(), ex.getReason() != null ? ex.getReason() : ex.getMessage());
        return ResponseEntity.status(ex.getStatusCode()).body(new ErrorApi(
                "ERROR_NEGOCIO",
                ex.getReason() != null ? ex.getReason() : ex.getMessage(),
                OffsetDateTime.now()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorApi> manejarGeneral(Exception ex) {
        log.error("Error interno no controlado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorApi(
                "ERROR_INTERNO",
                ex.getMessage(),
                OffsetDateTime.now()
        ));
    }
}
