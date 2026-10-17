package com.techprotech.agenda.modulos.citas.dominio.eventos;

/** Origen operativo, no rol ni permiso. El cobro interno de Caja se clasifica como ADMIN. */
public enum OrigenEventoCita {
    CLIENTE, RECEPCION, STAFF, ADMIN,
    /** El proveedor confirma el hecho; el iniciador de Point sigue identificado en la venta enlazada. */
    WEBHOOK,
    SISTEMA
}
