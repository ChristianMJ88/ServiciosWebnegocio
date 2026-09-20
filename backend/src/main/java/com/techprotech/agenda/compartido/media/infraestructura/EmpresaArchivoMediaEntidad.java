package com.techprotech.agenda.compartido.media.infraestructura;

import com.techprotech.agenda.compartido.media.dominio.CategoriaMedia;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "empresa_archivo_media")
public class EmpresaArchivoMediaEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaMedia categoria;
    @Column(nullable = false, length = 30)
    private String proveedor;
    @Column(name = "asset_id", length = 150)
    private String assetId;
    @Column(name = "public_id", nullable = false, length = 500)
    private String publicId;
    @Column(name = "secure_url", nullable = false, length = 1000)
    private String secureUrl;
    @Column(length = 30)
    private String formato;
    private Integer ancho;
    private Integer alto;
    @Column(nullable = false)
    private long bytes;
    @Column(name = "nombre_original", length = 255)
    private String nombreOriginal;
    @Column(name = "content_type", length = 120)
    private String contentType;
    @Column(name = "creado_por_usuario_id", nullable = false)
    private Long creadoPorUsuarioId;
    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void prepararCreacion() {
        if (creadoEn == null) creadoEn = LocalDateTime.now();
    }
}
