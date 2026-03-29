package com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class RolPermisoId implements Serializable {

    @Column(name = "rol_id")
    private Long rolId;

    @Column(name = "permiso_id")
    private Long permisoId;

    public RolPermisoId() {
    }

    public RolPermisoId(Long rolId, Long permisoId) {
        this.rolId = rolId;
        this.permisoId = permisoId;
    }

    public Long getRolId() {
        return rolId;
    }

    public Long getPermisoId() {
        return permisoId;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RolPermisoId that)) {
            return false;
        }
        return Objects.equals(rolId, that.rolId) && Objects.equals(permisoId, that.permisoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rolId, permisoId);
    }
}
