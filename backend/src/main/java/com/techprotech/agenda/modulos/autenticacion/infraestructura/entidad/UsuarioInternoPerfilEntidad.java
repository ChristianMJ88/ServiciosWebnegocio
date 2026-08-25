package com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "usuario_interno_perfil")
public class UsuarioInternoPerfilEntidad {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "sucursal_id")
    private Long sucursalId;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(length = 30)
    private String telefono;

    @Column(length = 80)
    private String puesto;

    @Column(length = 500)
    private String notas;

}
