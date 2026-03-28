package com.techprotech.agenda.modulos.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AsociarChannelSenderWhatsappRequest(
        @NotBlank @Size(max = 80) String channelSenderSid
) {
}
