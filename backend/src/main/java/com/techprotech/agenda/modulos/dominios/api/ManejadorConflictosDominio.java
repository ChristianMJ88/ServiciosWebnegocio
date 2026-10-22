package com.techprotech.agenda.modulos.dominios.api;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.dao.*;
import org.springframework.http.ResponseEntity;
import java.util.Map;
@RestControllerAdvice(basePackageClasses=ControladorDominiosAdmin.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ManejadorConflictosDominio {
 @ExceptionHandler({OptimisticLockingFailureException.class,PessimisticLockingFailureException.class})
 public ResponseEntity<Map<String,String>> conflicto(RuntimeException ignored){return ResponseEntity.status(409).body(Map.of("mensaje","La solicitud cambió durante otra operación. Actualiza e inténtalo de nuevo."));}
}
