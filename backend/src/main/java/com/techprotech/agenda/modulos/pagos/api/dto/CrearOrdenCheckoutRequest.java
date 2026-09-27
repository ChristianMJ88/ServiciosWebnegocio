package com.techprotech.agenda.modulos.pagos.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record CrearOrdenCheckoutRequest(
        @NotNull Long sucursalId,
        @Size(max = 60) String zonaPrecioClave,
        @NotBlank @Email @Size(max = 180) String correoCliente,
        @NotBlank @Size(max = 180) String nombreDestinatario,
        @NotBlank @Size(max = 30) String telefonoDestinatario,
        @NotBlank @Pattern(regexp = "RECOGE_EN_SUCURSAL|ENTREGA_LOCAL|ENVIO_NACIONAL") String modalidadEntrega,
        @Valid DireccionEntregaRequest direccion,
        @Valid @Size(max = 100) List<ProductoOrdenRequest> productos,
        @Size(max = 20) List<Long> citaIds
) {
    public record ProductoOrdenRequest(
            @NotNull Long productoId,
            @NotNull @DecimalMin("0.001") @Digits(integer = 11, fraction = 3) BigDecimal cantidad
    ) {}

    public record DireccionEntregaRequest(
            @NotBlank @Size(max = 180) String calle,
            @NotBlank @Size(max = 30) String numeroExterior,
            @Size(max = 30) String numeroInterior,
            @NotBlank @Size(max = 150) String colonia,
            @NotBlank @Size(max = 120) String estado,
            @NotBlank @Size(max = 150) String municipio,
            @NotBlank @Pattern(regexp = "[0-9]{5}") String codigoPostal,
            @Size(max = 500) String referencias
    ) {}
}
