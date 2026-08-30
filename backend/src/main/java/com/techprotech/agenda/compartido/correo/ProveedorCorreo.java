package com.techprotech.agenda.compartido.correo;

public enum ProveedorCorreo {
    SMTP,
    GRAPH,
    SENDGRID;

    public static ProveedorCorreo desdeValor(String valor, ProveedorCorreo valorPorDefecto) {
        if (valor == null || valor.isBlank()) {
            return valorPorDefecto;
        }

        return ProveedorCorreo.valueOf(valor.trim().toUpperCase());
    }
}
