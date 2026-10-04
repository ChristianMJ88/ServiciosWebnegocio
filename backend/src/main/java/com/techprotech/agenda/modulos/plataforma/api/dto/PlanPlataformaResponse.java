package com.techprotech.agenda.modulos.plataforma.api.dto;
import java.math.BigDecimal;
import java.util.List;
public record PlanPlataformaResponse(Long id, String codigo, String nombre, String descripcion, String moneda,
        BigDecimal precioMensual, BigDecimal precioAnual, boolean activo, boolean publico,
        List<ModuloPlanResponse> modulos) {}
