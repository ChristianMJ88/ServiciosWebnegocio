package com.techprotech.agenda.modulos.clientes.api.dto;

import java.util.List;

public record FiltrosClientesResponse(List<Opcion> ordenes, List<Opcion> citas, String ordenPredeterminado,
                                     String citasPredeterminado) {
    public record Opcion(String valor, String etiqueta) {}
}
