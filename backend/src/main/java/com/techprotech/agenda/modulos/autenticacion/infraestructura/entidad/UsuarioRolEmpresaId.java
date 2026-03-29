package com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

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

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Long getRolEmpresaId() {
        return rolEmpresaId;
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
