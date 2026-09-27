package com.techprotech.agenda.modulos.envios.api.dto;

import jakarta.validation.constraints.*;

public record OrigenEnvioRequest(
        @NotNull Long sucursalId,
        boolean activo,
        @NotBlank @Size(max = 180) String nombreContacto,
        @NotBlank @Size(max = 30) String telefono,
        @Email @Size(max = 180) String correo,
        @NotBlank @Size(max = 180) String calle,
        @NotBlank @Size(max = 30) String numeroExterior,
        @Size(max = 30) String numeroInterior,
        @NotBlank @Size(max = 150) String colonia,
        @NotBlank @Size(max = 150) String municipio,
        @NotBlank @Size(max = 120) String estado,
        @NotBlank @Pattern(regexp = "[0-9A-Za-z -]{3,12}") String codigoPostal,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}") String codigoPais,
        @Size(max = 500) String referencias
) {}
