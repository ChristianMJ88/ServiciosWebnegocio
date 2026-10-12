package com.techprotech.agenda.seguridad.jwt;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;

public final class EstadoSesionUsuario {
    private EstadoSesionUsuario() {}

    public static String motivoNoDisponible(UsuarioEntidad usuario, long versionToken) {
        if (usuario == null) return "Esta sesión ya no está disponible. Inicia sesión nuevamente.";
        if (usuario.isBloqueado()) return "El administrador bloqueó tu acceso. Contacta al administrador de tu negocio.";
        if (!usuario.isHabilitado()) return "Tu cuenta está inactiva. Contacta al administrador de tu negocio.";
        if (usuario.isRequiereCambioContrasena()) return "Debes cambiar tu contraseña temporal. Inicia sesión para crear tu contraseña personal.";
        if (usuario.getVersionSesion() != versionToken) return "Tu sesión fue revocada por un cambio de seguridad. Inicia sesión nuevamente.";
        return null;
    }
}
