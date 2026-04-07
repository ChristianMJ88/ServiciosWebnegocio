package com.techprotech.agenda.modulos.autenticacion.api.dto;

import java.util.List;

public record RespuestaAccesoApp(
        String estado,
        String mensaje,
        List<EmpresaAccesoAppResponse> empresas,
        RespuestaTokenJwt sesion
) {
}
