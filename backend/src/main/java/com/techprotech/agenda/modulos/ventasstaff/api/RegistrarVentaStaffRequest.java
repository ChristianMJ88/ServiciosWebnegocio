package com.techprotech.agenda.modulos.ventasstaff.api;
import com.techprotech.agenda.modulos.caja.api.dto.RegistrarVentaRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public record RegistrarVentaStaffRequest(@NotNull Long clienteId,@NotEmpty @Size(max=100) @Valid List<RegistrarVentaRequest.ProductoVentaRequest> productos) {}
