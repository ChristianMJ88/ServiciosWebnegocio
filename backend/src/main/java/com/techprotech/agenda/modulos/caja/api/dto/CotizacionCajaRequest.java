package com.techprotech.agenda.modulos.caja.api.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
public record CotizacionCajaRequest(@NotNull Long sucursalId, @NotNull @Valid List<RegistrarVentaRequest.ProductoVentaRequest> productos) {}
