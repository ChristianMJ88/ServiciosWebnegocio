package com.techprotech.agenda.compartido.correo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record ConfirmacionCitaCorreo(
        Long citaId,
        String nombreEmpresa,
        String nombreCliente,
        String destinatario,
        String servicioNombre,
        String sucursalNombre,
        String sucursalDireccion,
        String sucursalTelefono,
        OffsetDateTime inicio,
        OffsetDateTime fin,
        ZoneId zonaHoraria,
        BigDecimal precio,
        String moneda
) {
}
