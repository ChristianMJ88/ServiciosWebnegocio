package com.techprotech.agenda.modulos.admin.api.dto;

import java.util.List;

public record ImpactoPrestadorResponse(Long usuarioId, List<ConteoEstado> citasFuturas) {
    public record ConteoEstado(String estado, long cantidad) {}
}
