package com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Setter
@Getter
@AllArgsConstructor
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

