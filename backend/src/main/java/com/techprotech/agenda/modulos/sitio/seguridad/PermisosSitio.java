package com.techprotech.agenda.modulos.sitio.seguridad;

public final class PermisosSitio {
    public static final String PANEL_ADMIN = "PANEL_ADMIN_ACCESO";
    public static final String GESTIONAR = "CONFIGURACION_EMPRESA_GESTIONAR";
    public static final String EXPRESION_PANEL_ADMIN = "hasAuthority('" + PANEL_ADMIN + "')";
    public static final String EXPRESION_GESTIONAR = "hasAuthority('" + GESTIONAR + "')";

    private PermisosSitio() {
    }
}
