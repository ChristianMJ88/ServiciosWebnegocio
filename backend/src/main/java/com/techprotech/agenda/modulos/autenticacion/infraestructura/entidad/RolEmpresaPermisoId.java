package com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;
@Setter
@Getter
@Embeddable
public class RolEmpresaPermisoId implements Serializable {

    @Column(name = "rol_empresa_id")
    private Long rolEmpresaId;

    @Column(name = "permiso_id")
    private Long permisoId;

    public RolEmpresaPermisoId() {
    }

    public RolEmpresaPermisoId(Long rolEmpresaId, Long permisoId) {
        this.rolEmpresaId = rolEmpresaId;
        this.permisoId = permisoId;
    }

  @Override
    public boolean equals(Object o) {
        if (!(o instanceof RolEmpresaPermisoId that)) {
            return false;
        }
        return Objects.equals(rolEmpresaId, that.rolEmpresaId) && Objects.equals(permisoId, that.permisoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rolEmpresaId, permisoId);
    }
}
