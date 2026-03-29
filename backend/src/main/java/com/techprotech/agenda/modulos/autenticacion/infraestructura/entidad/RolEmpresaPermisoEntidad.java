package com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "rol_empresa_permiso")
public class RolEmpresaPermisoEntidad {

    @EmbeddedId
    private RolEmpresaPermisoId id;

    @ManyToOne(optional = false)
    @MapsId("rolEmpresaId")
    @JoinColumn(name = "rol_empresa_id")
    private RolEmpresaEntidad rolEmpresa;

    @ManyToOne(optional = false)
    @MapsId("permisoId")
    @JoinColumn(name = "permiso_id")
    private PermisoEntidad permiso;

    public RolEmpresaPermisoEntidad() {
    }

    public RolEmpresaPermisoEntidad(RolEmpresaPermisoId id, RolEmpresaEntidad rolEmpresa, PermisoEntidad permiso) {
        this.id = id;
        this.rolEmpresa = rolEmpresa;
        this.permiso = permiso;
    }

    public RolEmpresaPermisoId getId() {
        return id;
    }

    public RolEmpresaEntidad getRolEmpresa() {
        return rolEmpresa;
    }

    public PermisoEntidad getPermiso() {
        return permiso;
    }
}
