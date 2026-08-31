package com.techprotech.agenda.modulos.onboarding.api.dto;

public record RegistrarEmpresaResponse(
        Long empresaId,
        String slug,
        String nombreEmpresa,
        String correoAdministrador,
        String rutaSitioPublico,
        String rutaAcceso,
        boolean requiereConfirmacionCorreo
) {
}
