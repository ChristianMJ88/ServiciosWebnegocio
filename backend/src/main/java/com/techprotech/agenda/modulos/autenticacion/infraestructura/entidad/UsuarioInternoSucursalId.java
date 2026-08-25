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
public class UsuarioInternoSucursalId implements Serializable {

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "sucursal_id")
    private Long sucursalId;

    public UsuarioInternoSucursalId() {
    }

    public UsuarioInternoSucursalId(Long usuarioId, Long sucursalId) {
        this.usuarioId = usuarioId;
        this.sucursalId = sucursalId;
    }

  @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UsuarioInternoSucursalId that)) {
            return false;
        }
        return Objects.equals(usuarioId, that.usuarioId) && Objects.equals(sucursalId, that.sucursalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuarioId, sucursalId);
    }
}
