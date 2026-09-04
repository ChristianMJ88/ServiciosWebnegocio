package com.techprotech.agenda.modulos.parametros.api.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record GuardarParametroSistemaRequest(@NotBlank @Size(max = 500) String valor) {}
