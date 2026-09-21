package com.techprotech.agenda.modulos.sitio.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "empresa_sitio_resena")
public class EmpresaSitioResenaEntidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false)
    private int calificacion;

    @Column(nullable = false, length = 600)
    private String comentario;

    @Column(name = "media_id")
    private Long mediaId;

    @Column(name = "foto_url", length = 1000)
    private String fotoUrl;

    @Column(nullable = false)
    private boolean destacada;

    @Column(nullable = false)
    private boolean activa;

    @Column(nullable = false)
    private int orden;
}
