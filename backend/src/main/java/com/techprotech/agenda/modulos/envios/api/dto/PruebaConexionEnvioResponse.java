package com.techprotech.agenda.modulos.envios.api.dto;

public record PruebaConexionEnvioResponse(
        boolean conectada,
        String proveedor,
        String ambiente,
        String codigoPais,
        int paqueteriasDisponibles,
        String mensaje
) {}
