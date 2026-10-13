package com.techprotech.agenda.modulos.disponibilidad.api.dto;
import jakarta.validation.constraints.*;
public record ConfiguracionReservasResponse(
        @Min(0) @Max(10080) int anticipacionMinimaMinutos,
        @Min(1) @Max(365) int ventanaReservaDias,
        @Min(0) @Max(168) int liberacionSinConfirmacionHoras,
        @Min(1) @Max(1440) int minutosApartadoPago,
        @Min(1) @Max(168) Integer horasTopeProcesamientoPago) {
    public ConfiguracionReservasResponse(int anticipacion, int ventana, int liberacion, int apartado) {
        this(anticipacion, ventana, liberacion, apartado, null);
    }
}
