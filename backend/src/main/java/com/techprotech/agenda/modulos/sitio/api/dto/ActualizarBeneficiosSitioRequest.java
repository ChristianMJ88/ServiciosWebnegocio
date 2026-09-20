package com.techprotech.agenda.modulos.sitio.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ActualizarBeneficiosSitioRequest(
        @NotNull @Size(max = 8) List<@Valid BeneficioSitioRequest> beneficios
) {
}
