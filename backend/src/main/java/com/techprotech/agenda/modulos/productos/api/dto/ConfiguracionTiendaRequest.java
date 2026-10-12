package com.techprotech.agenda.modulos.productos.api.dto;

import jakarta.validation.constraints.Size;

public record ConfiguracionTiendaRequest(
        boolean habilitada,
        boolean menuMostrarProductos,
        @Size(max = 160) String titulo,
        @Size(max = 500) String descripcion,
        @Size(max = 70) String seoTitulo,
        @Size(max = 170) String seoDescripcion,
        @Size(max = 40) String textoAgregar,
        boolean carritoHabilitado,
        boolean mostrarStock,
        boolean permitirPedidoWhatsapp,
        @jakarta.validation.constraints.Min(0) int diasNuevo
) {
}
