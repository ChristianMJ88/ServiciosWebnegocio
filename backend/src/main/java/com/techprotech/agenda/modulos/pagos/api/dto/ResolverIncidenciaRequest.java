package com.techprotech.agenda.modulos.pagos.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public record ResolverIncidenciaRequest(@NotNull Tipo tipo, @NotBlank @Size(max=500) String motivo,
                                        @Valid List<Reprogramacion> citas) {
    public enum Tipo { REPROGRAMADA, REEMBOLSADA_FUERA_DEL_SISTEMA, OTRA }
    public record Reprogramacion(@NotNull Long citaId, @NotNull LocalDateTime inicio) {}
}
