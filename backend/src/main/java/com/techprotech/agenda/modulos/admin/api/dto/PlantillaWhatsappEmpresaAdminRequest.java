package com.techprotech.agenda.modulos.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlantillaWhatsappEmpresaAdminRequest(
        @NotBlank @Size(max = 120) String nombre,
        @NotBlank @Size(max = 80) String uso,
        @NotBlank @Size(max = 80) String contentSid,
        @Size(max = 80) String tipoContenido,
        @Size(max = 40) String categoria,
        @Size(max = 40) String estado,
        boolean activa
) {
}
