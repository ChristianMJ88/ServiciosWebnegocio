package com.techprotech.agenda.modulos.dominios.api.dto;
import jakarta.validation.constraints.*;
public record DominioSolicitud(@NotBlank @Size(max=253) String hostname) {}
