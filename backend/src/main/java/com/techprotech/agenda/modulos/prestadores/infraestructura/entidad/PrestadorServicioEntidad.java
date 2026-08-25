package com.techprotech.agenda.modulos.prestadores.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "prestador_servicio")
public class PrestadorServicioEntidad {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "sucursal_id", nullable = false)
    private Long sucursalId;

    @Column(name = "nombre_mostrar", nullable = false, length = 150)
    private String nombreMostrar;

    @Column(length = 500)
    private String biografia;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "color_agenda", length = 20)
    private String colorAgenda;

}
