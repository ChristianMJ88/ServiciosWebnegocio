package com.techprotech.agenda.modulos.caja.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record RegistrarVentaRequest(
        @NotNull Long sucursalId,
        Long citaId,
        Long clienteId,
        @Valid @Size(max = 100) List<ProductoVentaRequest> productos,
        @NotBlank String metodoPago,
        @Size(max = 120) String referencia,
        @Size(max = 500) String observaciones,
        Long ventaPendienteId,
        @Size(max = 20) List<@NotNull Long> citaIds
) {
    public RegistrarVentaRequest(Long sucursalId,Long citaId,Long clienteId,List<ProductoVentaRequest> productos,String metodoPago,String referencia,String observaciones, Long ventaPendienteId) {
        this(sucursalId,citaId,clienteId,productos,metodoPago,referencia,observaciones,ventaPendienteId,null);
    }
    public List<Long> citasNormalizadas() {
        if (citaId != null && citaIds != null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Usa citaId o citaIds, no ambos");
        var lista = citaIds == null ? (citaId == null ? List.<Long>of() : List.of(citaId)) : citaIds;
        if (lista.size() > 20 || lista.stream().anyMatch(java.util.Objects::isNull)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Indica hasta veinte citas válidas");
        return lista.stream().distinct().sorted().toList();
    }
    public RegistrarVentaRequest(Long sucursalId,Long citaId,Long clienteId,List<ProductoVentaRequest> productos,String metodoPago,String referencia,String observaciones) {
        this(sucursalId,citaId,clienteId,productos,metodoPago,referencia,observaciones,null);
    }
    public record ProductoVentaRequest(
            @NotNull Long productoId,
            @NotNull @DecimalMin("0.001") BigDecimal cantidad
    ) {}
}
