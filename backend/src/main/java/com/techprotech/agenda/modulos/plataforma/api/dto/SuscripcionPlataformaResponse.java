package com.techprotech.agenda.modulos.plataforma.api.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record SuscripcionPlataformaResponse(Long id, Long empresaId, String empresaNombre, Long planId,
        String planNombre, String estado, String ciclo, BigDecimal precioAcordado, String moneda,
        LocalDateTime iniciaEn, LocalDateTime terminaEn, LocalDateTime pruebaHasta,
        Long proveedorPagoId, String referenciaExterna) {}
