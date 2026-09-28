package com.techprotech.agenda.modulos.envios.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record CotizarEnvioPublicoRequest(
        @NotNull Long sucursalId,
        @Size(max = 60) String zonaPrecioClave,
        @NotBlank @Size(max = 180) String nombreDestinatario,
        @NotBlank @Email @Size(max = 180) String correoDestinatario,
        @NotBlank @Size(max = 30) String telefonoDestinatario,
        @NotNull @Valid Direccion direccion,
        @NotEmpty @Size(max = 100) List<@Valid Producto> productos
) {
    public record Direccion(
            @NotBlank @Size(max = 180) String calle,
            @NotBlank @Size(max = 30) String numeroExterior,
            @Size(max = 30) String numeroInterior,
            @NotBlank @Size(max = 150) String colonia,
            @NotBlank @Size(max = 150) String municipio,
            @NotBlank @Size(max = 120) String estado,
            @NotBlank @Pattern(regexp = "[0-9]{5}") String codigoPostal,
            @NotBlank @Pattern(regexp = "[A-Za-z]{2}") String codigoPais,
            @Size(max = 500) String referencias
    ) {}

    public record Producto(
            @NotNull Long productoId,
            @NotNull @DecimalMin("0.001") @Digits(integer = 11, fraction = 3) BigDecimal cantidad
    ) {}
}
