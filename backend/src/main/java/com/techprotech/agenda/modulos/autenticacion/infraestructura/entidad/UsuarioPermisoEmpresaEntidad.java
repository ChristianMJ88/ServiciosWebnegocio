package com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "usuario_permiso_empresa")
public class UsuarioPermisoEmpresaEntidad {

    @EmbeddedId
    private UsuarioPermisoEmpresaId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("usuarioId")
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntidad usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("permisoId")
    @JoinColumn(name = "permiso_id", nullable = false)
    private PermisoEntidad permiso;

    public UsuarioPermisoEmpresaEntidad() {
    }

    public UsuarioPermisoEmpresaEntidad(UsuarioPermisoEmpresaId id, UsuarioEntidad usuario, PermisoEntidad permiso) {
        this.id = id;
        this.usuario = usuario;
        this.permiso = permiso;
    }

}
