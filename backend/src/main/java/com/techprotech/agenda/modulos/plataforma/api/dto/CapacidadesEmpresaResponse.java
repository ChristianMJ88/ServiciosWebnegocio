package com.techprotech.agenda.modulos.plataforma.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CapacidadesEmpresaResponse(
        boolean suscripcionVigente,Long suscripcionId,Long planId,String planCodigo,String planNombre,String estado,
        LocalDateTime pruebaHasta,LocalDateTime periodoActualHasta,
        List<CapacidadModuloEmpresaResponse> modulos,List<CapacidadLimiteEmpresaResponse> limites) {
    public static CapacidadesEmpresaResponse sinSuscripcion() {
        return new CapacidadesEmpresaResponse(false,null,null,null,null,"SIN_SUSCRIPCION",null,null,List.of(),List.of());
    }
}
