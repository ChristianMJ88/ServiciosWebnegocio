package com.techprotech.agenda.modulos.pagos.seguridad;

public final class PermisosPagos {
    public static final String CONFIGURAR = "PAGOS_CONFIGURAR";
    public static final String EXPRESION_CONFIGURAR = "hasAuthority('" + CONFIGURAR + "')";

    private PermisosPagos() {}
}
