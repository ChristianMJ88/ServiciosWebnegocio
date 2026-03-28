package com.techprotech.agenda.modulos.admin.api.dto;

import jakarta.validation.constraints.Size;

public record ProvisionarSubcuentaWhatsappRequest(
        @Size(max = 64) String friendlyName
) {
}
