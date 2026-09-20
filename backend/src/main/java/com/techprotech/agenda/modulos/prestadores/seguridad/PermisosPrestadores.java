package com.techprotech.agenda.modulos.prestadores.seguridad;

public final class PermisosPrestadores {
    public static final String GESTIONAR = "PRESTADORES_GESTIONAR";
    public static final String EXPRESION_GESTIONAR = "hasAuthority('" + GESTIONAR + "')";

    private PermisosPrestadores() {}
}
