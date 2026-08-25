package com.techprotech.agenda.modulos.servicios.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "grupo_servicio")
public class GrupoServicioEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 140)
    private String slug;

    @Column(length = 300)
    private String descripcion;

    @Column(name = "imagen_url", length = 255)
    private String imagenUrl;

    @Column(length = 50)
    private String icono;

    @Column(name = "orden_publico", nullable = false)
    private int ordenPublico;

    @Column(nullable = false)
    private boolean activo;


}
