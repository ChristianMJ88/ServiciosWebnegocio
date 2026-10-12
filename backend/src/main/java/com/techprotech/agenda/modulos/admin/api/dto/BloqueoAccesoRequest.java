package com.techprotech.agenda.modulos.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BloqueoAccesoRequest(boolean bloqueado, @NotBlank @Size(max = 500) String motivo) {}
