package com.techprotech.agenda.modulos.autenticacion.social;

public record ConfiguracionSocialResponse(
        boolean google,
        boolean microsoft,
        boolean apple,
        String googleInicioUrl,
        String googleAccesoUrl,
        String microsoftInicioUrl,
        String microsoftAccesoUrl
) {
}
