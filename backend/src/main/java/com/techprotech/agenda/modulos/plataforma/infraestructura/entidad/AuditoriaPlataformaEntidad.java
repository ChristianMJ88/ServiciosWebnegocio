package com.techprotech.agenda.modulos.plataforma.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @Entity
@Table(name = "plataforma_auditoria")
public class AuditoriaPlataformaEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "usuario_id", nullable = false) private Long usuarioId;
    @Column(nullable = false, length = 100) private String accion;
    @Column(nullable = false, length = 100) private String recurso;
    @Column(name = "recurso_id", length = 120) private String recursoId;
    @Column(columnDefinition = "json") private String detalle;
    @Column(name = "creada_en", nullable = false, insertable = false, updatable = false) private LocalDateTime creadaEn;
}
