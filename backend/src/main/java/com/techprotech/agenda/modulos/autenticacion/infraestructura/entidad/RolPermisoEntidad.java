package com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "rol_permiso")
public class RolPermisoEntidad {

    @EmbeddedId
    private RolPermisoId id;

    @ManyToOne(optional = false)
    @MapsId("rolId")
    @JoinColumn(name = "rol_id")
    private RolEntidad rol;

    @ManyToOne(optional = false)
    @MapsId("permisoId")
    @JoinColumn(name = "permiso_id")
    private PermisoEntidad permiso;

    public RolPermisoEntidad() {
    }

    public RolPermisoEntidad(RolPermisoId id, RolEntidad rol, PermisoEntidad permiso) {
        this.id = id;
        this.rol = rol;
        this.permiso = permiso;
    }

    public RolPermisoId getId() {
        return id;
    }

    public RolEntidad getRol() {
        return rol;
    }

    public PermisoEntidad getPermiso() {
        return permiso;
    }
}
