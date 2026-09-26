package com.techprotech.agenda.modulos.pagos.dominio;

import java.util.Map;

public record WebhookProveedorPago(
        Map<String, String> encabezados,
        Map<String, String> parametros,
        String payload
) {
}
