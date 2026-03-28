package com.techprotech.agenda.modulos.admin.api.dto;

import jakarta.validation.constraints.Size;

public record ProvisionarMessagingServiceWhatsappRequest(
        @Size(max = 64) String friendlyName,
        @Size(max = 255) String inboundRequestUrl
) {
}
