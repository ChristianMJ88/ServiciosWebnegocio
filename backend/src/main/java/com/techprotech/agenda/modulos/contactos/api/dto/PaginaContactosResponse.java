package com.techprotech.agenda.modulos.contactos.api.dto;

import java.util.List;

public record PaginaContactosResponse(
        List<SolicitudContactoAdminResponse> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas
) {
}
