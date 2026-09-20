package com.techprotech.agenda.modulos.sitio.aplicacion;

import java.math.BigDecimal;

public record ServicioDestacadoPublicoItemResponse(
        String tipo,
        Long referenciaId,
        Long grupoId,
        Long subgrupoId,
        Long servicioId,
        String titulo,
        String descripcion,
        String imagenUrl,
        BigDecimal precioDesde,
        String moneda,
        Integer duracionMinutos,
        int cantidadServicios,
        String etiqueta
) {
}
