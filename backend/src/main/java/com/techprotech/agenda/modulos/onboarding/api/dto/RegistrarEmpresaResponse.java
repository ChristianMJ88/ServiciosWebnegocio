package com.techprotech.agenda.modulos.onboarding.api.dto;

import com.techprotech.agenda.modulos.autenticacion.api.dto.RespuestaTokenJwt;

public record RegistrarEmpresaResponse(
        Long empresaId,
        String slug,
        String nombreEmpresa,
        String correoAdministrador,
        String rutaSitioPublico,
        String rutaAcceso,
        boolean requiereConfirmacionCorreo,
        RespuestaTokenJwt sesion
) {
}
