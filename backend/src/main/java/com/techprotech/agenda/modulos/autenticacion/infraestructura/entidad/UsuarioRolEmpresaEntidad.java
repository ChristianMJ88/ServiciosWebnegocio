package com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario_rol_empresa")
public class UsuarioRolEmpresaEntidad {

    @EmbeddedId
    private UsuarioRolEmpresaId id;

    @ManyToOne(optional = false)
    @MapsId("usuarioId")
    @JoinColumn(name = "usuario_id")
    private UsuarioEntidad usuario;

    @ManyToOne(optional = false)
    @MapsId("rolEmpresaId")
    @JoinColumn(name = "rol_empresa_id")
    private RolEmpresaEntidad rolEmpresa;

    public UsuarioRolEmpresaEntidad() {
    }

    public UsuarioRolEmpresaEntidad(UsuarioRolEmpresaId id, UsuarioEntidad usuario, RolEmpresaEntidad rolEmpresa) {
        this.id = id;
        this.usuario = usuario;
        this.rolEmpresa = rolEmpresa;
    }

    public UsuarioRolEmpresaId getId() {
        return id;
    }

    public UsuarioEntidad getUsuario() {
        return usuario;
    }

    public RolEmpresaEntidad getRolEmpresa() {
        return rolEmpresa;
    }
}
