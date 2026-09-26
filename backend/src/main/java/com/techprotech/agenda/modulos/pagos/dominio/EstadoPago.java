package com.techprotech.agenda.modulos.pagos.dominio;

public enum EstadoPago {
    CREADO,
    REQUIERE_ACCION,
    PROCESANDO,
    APROBADO,
    RECHAZADO,
    CANCELADO,
    REEMBOLSADO,
    DESCONOCIDO
}
