package com.techprotech.agenda.modulos.contactos.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Setter
@Getter
@Entity
@Table(name = "solicitud_contacto")
public class SolicitudContactoEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(length = 30)
    private String telefono;

    @Column(nullable = false, length = 150)
    private String correo;

    @Column(nullable = false, length = 180)
    private String asunto;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Column(nullable = false, length = 30)
    private String canal;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "notificacion_correo_programada", nullable = false)
    private boolean notificacionCorreoProgramada;

    @Column(name = "notificada_en")
    private LocalDateTime notificadaEn;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private LocalDateTime creadaEn;

    @PrePersist
    void inicializarCamposAutomaticos() {
        if (creadaEn == null) {
            creadaEn = LocalDateTime.now();
        }
    }

}
