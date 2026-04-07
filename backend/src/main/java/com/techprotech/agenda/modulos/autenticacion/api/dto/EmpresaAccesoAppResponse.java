package com.techprotech.agenda.modulos.autenticacion.api.dto;

import java.util.List;

public record EmpresaAccesoAppResponse(
        Long empresaId,
        String empresaSlug,
        String empresaNombre,
        List<String> roles,
        List<String> permisos
) {
}
