package com.techprotech.agenda.modulos.plataforma.api.dto;
import jakarta.validation.constraints.*;
public record CorreoFacturacionRequest(@NotBlank @Email @Size(max=150) String correo) {}
