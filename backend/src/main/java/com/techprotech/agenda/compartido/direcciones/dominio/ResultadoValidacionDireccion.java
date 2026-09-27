package com.techprotech.agenda.compartido.direcciones.dominio;

import java.math.BigDecimal;

public record ResultadoValidacionDireccion(
        boolean valida,
        String estadoValidacion,
        String mensaje,
        String direccionFormateada,
        Direccion direccion,
        String placeId,
        BigDecimal latitud,
        BigDecimal longitud
) {}
