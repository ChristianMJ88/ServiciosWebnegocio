package com.techprotech.agenda.modulos.sitio.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "empresa_sitio_galeria_imagen")
public class EmpresaSitioGaleriaImagenEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;
    @Column(name = "media_id")
    private Long mediaId;
    @Column(name = "imagen_url", nullable = false, length = 500)
    private String imagenUrl;
    @Column(name = "texto_alternativo", nullable = false, length = 180)
    private String textoAlternativo;
    @Column(length = 120)
    private String titulo;
    @Column(length = 500)
    private String descripcion;
    @Column(name = "servicio_id")
    private Long servicioId;
    @Column(name = "sucursal_id")
    private Long sucursalId;
    @Column(nullable = false)
    private boolean activa;
    @Column(nullable = false)
    private int orden;
}
