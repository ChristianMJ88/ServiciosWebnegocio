package com.techprotech.agenda.modulos.sitio.infraestructura.entidad;

import com.techprotech.agenda.modulos.sitio.dominio.VistaGaleriaSitio;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "empresa_sitio_galeria_config")
public class EmpresaSitioGaleriaConfigEntidad {
    @Id
    @Column(name = "empresa_id")
    private Long empresaId;
    @Column(nullable = false, length = 120)
    private String titulo;
    @Column(length = 500)
    private String descripcion;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VistaGaleriaSitio vista;
    @Column(name = "ampliacion_habilitada", nullable = false)
    private boolean ampliacionHabilitada;
}
