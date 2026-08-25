package com.techprotech.agenda.compartido.whatsapp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "whatsapp_plantilla_empresa")
public class PlantillaWhatsappEmpresaEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 80)
    private String uso;

    @Column(name = "content_sid", nullable = false, length = 80)
    private String contentSid;

    @Column(name = "tipo_contenido", length = 80)
    private String tipoContenido;

    @Column(length = 40)
    private String categoria;

    @Column(length = 40)
    private String estado;

    @Column(nullable = false)
    private boolean activa = true;

    @Column(name = "creada_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime creadaEn;

    @Column(name = "actualizada_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime actualizadaEn;
}
