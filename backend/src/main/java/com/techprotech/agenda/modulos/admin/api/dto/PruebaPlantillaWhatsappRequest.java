package com.techprotech.agenda.modulos.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PruebaPlantillaWhatsappRequest(
        @NotBlank @Pattern(regexp = "^[0-9+ ]{10,15}$") String telefonoDestino,
        @NotBlank @Size(min = 3, max = 150) String nombreCliente,
        @NotBlank @Size(max = 30) String fecha,
        @NotBlank @Size(max = 10) String hora,
        @Size(max = 80) String plantillaSid
) {
}
