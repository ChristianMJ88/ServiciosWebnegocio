package com.techprotech.agenda.modulos.clientes.api.dto;

import java.util.List;

public record PaginaClientesResponse(
        List<ClienteResponse> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas
) {
}
