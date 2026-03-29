package com.techprotech.agenda.seguridad.jwt;

import java.util.List;

public record UsuarioAutenticado(
        String correo,
        Long usuarioId,
        Long empresaId,
        List<String> roles,
        List<String> permisos,
        List<Long> sucursalesPermitidas
) {
    public boolean tienePermiso(String permiso) {
        return permisos != null && permisos.contains(permiso);
    }

    public boolean tieneScopeSucursales() {
        return sucursalesPermitidas != null && !sucursalesPermitidas.isEmpty();
    }

    public boolean puedeAccederSucursal(Long sucursalId) {
        return !tieneScopeSucursales() || (sucursalId != null && sucursalesPermitidas.contains(sucursalId));
    }
}
