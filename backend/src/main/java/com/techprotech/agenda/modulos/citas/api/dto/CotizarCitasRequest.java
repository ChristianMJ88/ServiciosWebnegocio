package com.techprotech.agenda.modulos.citas.api.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.List;

public record CotizarCitasRequest(Long empresaId, @NotNull Long sucursalId, Long promocionId,
                                 @NotEmpty @Size(max=20) List<@Valid ItemCotizacion> items,
                                 @Size(max=100) List<@Valid Producto> productos) {
    public CotizarCitasRequest(Long empresaId,Long sucursalId,Long promocionId,List<ItemCotizacion> items) { this(empresaId,sucursalId,promocionId,items,null); }
    public record Producto(@NotNull Long productoId,@NotNull @DecimalMin("0.01") java.math.BigDecimal cantidad) {}
    // A preview may specify the desired branch-local day before choosing a slot.
    // Creation always uses the actual validated start, never this preview date.
    public record ItemCotizacion(@NotNull Long servicioId, OffsetDateTime inicio, java.time.LocalDate fecha) {
        public ItemCotizacion(Long servicioId, OffsetDateTime inicio) { this(servicioId,inicio,null); }
    }
}
