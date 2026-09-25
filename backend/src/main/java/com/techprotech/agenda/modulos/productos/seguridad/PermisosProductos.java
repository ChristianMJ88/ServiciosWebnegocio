package com.techprotech.agenda.modulos.productos.seguridad;

public final class PermisosProductos {
    public static final String GESTIONAR = "PRODUCTOS_GESTIONAR";
    public static final String INVENTARIO = "INVENTARIO_GESTIONAR";
    public static final String EXPRESION_GESTIONAR = "hasAuthority('" + GESTIONAR + "')";
    public static final String EXPRESION_INVENTARIO = "hasAuthority('" + INVENTARIO + "')";

    private PermisosProductos() {}
}
