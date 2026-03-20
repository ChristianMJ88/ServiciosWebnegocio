package com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UsuarioRolId implements Serializable {

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "rol_id")
    private Long rolId;

    @Column(name = "empresa_id")
    private Long empresaId;

    public UsuarioRolId() {
    }

    public UsuarioRolId(Long usuarioId, Long rolId, Long empresaId) {
        this.usuarioId = usuarioId;
        this.rolId = rolId;
        this.empresaId = empresaId;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Long getRolId() {
        return rolId;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsuarioRolId that)) return false;
        return Objects.equals(usuarioId, that.usuarioId)
                && Objects.equals(rolId, that.rolId)
                && Objects.equals(empresaId, that.empresaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuarioId, rolId, empresaId);
    }
}

