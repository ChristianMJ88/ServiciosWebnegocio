package com.techprotech.agenda.modulos.caja.api;

@org.springframework.web.bind.annotation.RestControllerAdvice(basePackages="com.techprotech.agenda.modulos.caja.api")
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public class ManejadorConcurrenciaCaja {
    private final java.time.Clock reloj;
    public ManejadorConcurrenciaCaja(java.time.Clock reloj) {this.reloj=reloj;}
    @org.springframework.web.bind.annotation.ExceptionHandler(org.springframework.dao.PessimisticLockingFailureException.class)
    public org.springframework.http.ResponseEntity<com.techprotech.agenda.compartido.exception.ErrorApi> conflicto(Exception ex) {
        return org.springframework.http.ResponseEntity.status(409).body(new com.techprotech.agenda.compartido.exception.ErrorApi(
            "COBRO_CONCURRENTE","La operación coincidió con otro cobro. Intenta de nuevo.",java.time.OffsetDateTime.now(reloj)));
    }
}
