package com.techprotech.agenda.modulos.servicios.seguridad;

public final class PermisosServicios {
    public static final String GESTIONAR = "SERVICIOS_GESTIONAR";
    public static final String EXPRESION_GESTIONAR = "hasAuthority('" + GESTIONAR + "')";

    private PermisosServicios() {}
}
