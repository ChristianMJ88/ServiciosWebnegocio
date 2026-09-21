package com.techprotech.agenda.modulos.sitio.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record ActualizarPreguntasFrecuentesSitioRequest(
        @NotNull @Size(max = 20) List<@Valid PreguntaFrecuenteSitioRequest> preguntas
) {}
