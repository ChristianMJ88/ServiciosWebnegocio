package com.techprotech.agenda.modulos.sitio.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ActualizarPromocionesSitioRequest(
        @NotNull @Size(max = 24) List<@Valid PromocionSitioRequest> promociones
) {}
