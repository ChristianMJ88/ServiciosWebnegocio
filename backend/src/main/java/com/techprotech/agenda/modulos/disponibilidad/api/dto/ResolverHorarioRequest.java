package com.techprotech.agenda.modulos.disponibilidad.api.dto;
import jakarta.validation.constraints.NotBlank;
public record ResolverHorarioRequest(boolean aprobar, @NotBlank String contrasena) {}
