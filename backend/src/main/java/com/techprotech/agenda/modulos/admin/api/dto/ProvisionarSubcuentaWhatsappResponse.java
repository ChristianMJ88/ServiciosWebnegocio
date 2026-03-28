package com.techprotech.agenda.modulos.admin.api.dto;

public record ProvisionarSubcuentaWhatsappResponse(
        boolean creada,
        String mensaje,
        String friendlyName,
        String subaccountSid,
        String estado,
        ConfiguracionWhatsappAdminResponse configuracion
) {
}
