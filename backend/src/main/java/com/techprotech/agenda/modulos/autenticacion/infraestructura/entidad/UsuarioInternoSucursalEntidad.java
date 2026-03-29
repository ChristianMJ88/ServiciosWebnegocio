package com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad;

import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario_interno_sucursal")
public class UsuarioInternoSucursalEntidad {

    @EmbeddedId
    private UsuarioInternoSucursalId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("usuarioId")
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntidad usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("sucursalId")
    @JoinColumn(name = "sucursal_id", nullable = false)
    private SucursalEntidad sucursal;

    public UsuarioInternoSucursalEntidad() {
    }

    public UsuarioInternoSucursalEntidad(UsuarioInternoSucursalId id, UsuarioEntidad usuario, SucursalEntidad sucursal) {
        this.id = id;
        this.usuario = usuario;
        this.sucursal = sucursal;
    }

    public UsuarioInternoSucursalId getId() {
        return id;
    }

    public UsuarioEntidad getUsuario() {
        return usuario;
    }

    public SucursalEntidad getSucursal() {
        return sucursal;
    }
}
