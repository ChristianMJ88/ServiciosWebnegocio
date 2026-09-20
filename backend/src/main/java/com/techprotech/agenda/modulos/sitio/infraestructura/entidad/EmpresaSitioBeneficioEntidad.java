package com.techprotech.agenda.modulos.sitio.infraestructura.entidad;

import com.techprotech.agenda.modulos.sitio.dominio.IconoBeneficioSitio;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "empresa_sitio_beneficio")
public class EmpresaSitioBeneficioEntidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, length = 400)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IconoBeneficioSitio icono;

    @Column(nullable = false)
    private boolean activo;

    @Column(nullable = false)
    private int orden;
}
