package com.techprotech.agenda.modulos.marketing.api;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import java.util.Map;
@RestControllerAdvice(assignableTypes=ControladorSoportePlataforma.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ManejadorSolicitudSoporte {
 @ExceptionHandler(HttpMessageNotReadableException.class)
 public ResponseEntity<Map<String,String>> ilegible(){return ResponseEntity.badRequest().body(Map.of("codigo","VALIDACION_INVALIDA","mensaje","Revisa los tipos de datos y el formato de las horas del contacto"));}
}
