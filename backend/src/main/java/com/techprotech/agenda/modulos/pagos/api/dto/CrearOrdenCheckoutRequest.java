package com.techprotech.agenda.modulos.pagos.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record CrearOrdenCheckoutRequest(
        @NotNull Long sucursalId,
        @Size(max = 60) String zonaPrecioClave,
        @NotBlank @Email @Size(max = 180) String correoCliente,
        @Valid @Size(max = 100) List<ProductoOrdenRequest> productos,
        @Size(max = 20) List<Long> citaIds
) {
    public record ProductoOrdenRequest(
            @NotNull Long productoId,
            @NotNull @DecimalMin("0.001") @Digits(integer = 11, fraction = 3) BigDecimal cantidad
    ) {}
}
