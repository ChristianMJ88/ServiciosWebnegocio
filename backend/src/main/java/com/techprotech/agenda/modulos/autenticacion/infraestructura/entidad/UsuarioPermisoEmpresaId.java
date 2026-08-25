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
public class UsuarioPermisoEmpresaId implements Serializable {

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "permiso_id")
    private Long permisoId;

    public UsuarioPermisoEmpresaId() {
    }

    public UsuarioPermisoEmpresaId(Long usuarioId, Long permisoId) {
        this.usuarioId = usuarioId;
        this.permisoId = permisoId;
    }

  @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UsuarioPermisoEmpresaId that)) {
            return false;
        }
        return Objects.equals(usuarioId, that.usuarioId) && Objects.equals(permisoId, that.permisoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuarioId, permisoId);
    }
}
