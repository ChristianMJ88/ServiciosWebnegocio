package com.techprotech.agenda.modulos.plataforma.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ConfigurarSeccionesModuloRequest(@NotNull List<@NotBlank String> secciones) {}
