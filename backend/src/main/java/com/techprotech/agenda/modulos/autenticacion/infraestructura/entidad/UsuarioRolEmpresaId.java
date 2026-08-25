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
public class UsuarioRolEmpresaId implements Serializable {

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "rol_empresa_id")
    private Long rolEmpresaId;

    public UsuarioRolEmpresaId() {
    }

    public UsuarioRolEmpresaId(Long usuarioId, Long rolEmpresaId) {
        this.usuarioId = usuarioId;
        this.rolEmpresaId = rolEmpresaId;
    }

  @Override
    public boolean equals(Object o) {
        if (!(o instanceof UsuarioRolEmpresaId that)) {
            return false;
        }
        return Objects.equals(usuarioId, that.usuarioId) && Objects.equals(rolEmpresaId, that.rolEmpresaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuarioId, rolEmpresaId);
    }
}
