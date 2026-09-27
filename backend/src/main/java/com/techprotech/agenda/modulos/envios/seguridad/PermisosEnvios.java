package com.techprotech.agenda.modulos.envios.seguridad;

public final class PermisosEnvios {
    public static final String CONFIGURAR = "ENVIOS_CONFIGURAR";
    public static final String EXPRESION_CONFIGURAR = "hasAuthority('" + CONFIGURAR + "')";

    private PermisosEnvios() {}
}
