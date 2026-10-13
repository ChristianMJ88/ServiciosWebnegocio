package com.techprotech.agenda.modulos.pagos.api.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
public record IncidenciaCheckoutResponse(String ordenId, Long sucursalId, String motivo,
        BigDecimal importe, String moneda, LocalDateTime creadaEn, List<Cita> citas,
        List<ResolverIncidenciaRequest.Tipo> resoluciones) {
    public record Cita(Long citaId, LocalDateTime inicio) {}
}
