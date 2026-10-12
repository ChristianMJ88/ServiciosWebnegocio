package com.techprotech.agenda.modulos.productos.api.dto;

public record ConfiguracionTiendaResponse(
        boolean habilitada,
        boolean menuMostrarProductos,
        String titulo,
        String descripcion,
        String seoTitulo,
        String seoDescripcion,
        String textoAgregar,
        boolean carritoHabilitado,
        boolean mostrarStock,
        boolean permitirPedidoWhatsapp,
        int diasNuevo
) {
}
