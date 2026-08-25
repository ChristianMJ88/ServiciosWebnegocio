package com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@AllArgsConstructor
@Table(name = "usuario_rol")
public class UsuarioRolEntidad {

    @EmbeddedId
    private UsuarioRolId id;

    @ManyToOne(optional = false)
    @MapsId("usuarioId")
    @JoinColumn(name = "usuario_id")
    private UsuarioEntidad usuario;

    @ManyToOne(optional = false)
    @MapsId("rolId")
    @JoinColumn(name = "rol_id")
    private RolEntidad rol;

    public UsuarioRolEntidad() {
    }
}

