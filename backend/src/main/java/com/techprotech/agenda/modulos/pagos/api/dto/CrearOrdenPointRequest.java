package com.techprotech.agenda.modulos.pagos.api.dto;

import com.techprotech.agenda.modulos.caja.api.dto.RegistrarVentaRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CrearOrdenPointRequest(@NotNull Long terminalId, @NotNull @Valid RegistrarVentaRequest venta) {}
