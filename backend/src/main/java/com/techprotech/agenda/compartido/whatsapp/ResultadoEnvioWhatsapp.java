package com.techprotech.agenda.compartido.whatsapp;

public record ResultadoEnvioWhatsapp(
        String proveedorMensajeId,
        String estadoProveedor,
        String codigoErrorProveedor,
        String detalleErrorProveedor
) {
}
