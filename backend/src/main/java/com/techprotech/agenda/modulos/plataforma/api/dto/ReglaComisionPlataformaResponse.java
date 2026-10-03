package com.techprotech.agenda.modulos.plataforma.api.dto;

import com.techprotech.agenda.modulos.pagos.dominio.TipoComisionPlataforma;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReglaComisionPlataformaResponse(Long id, Long proveedorId, String proveedorNombre,
        Long empresaId, String empresaNombre, TipoComisionPlataforma tipo, BigDecimal valor,
        BigDecimal importeMinimo, BigDecimal importeMaximo, String moneda, int prioridad,
        LocalDateTime vigenteDesde, LocalDateTime vigenteHasta, boolean activa) {}
