package com.techprotech.agenda.modulos.sitio.infraestructura.entidad;

import com.techprotech.agenda.modulos.sitio.dominio.IconoIndicadorConfianza;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "empresa_sitio_indicador_confianza")
public class EmpresaSitioIndicadorConfianzaEntidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false, length = 40)
    private String valor;

    @Column(nullable = false, length = 80)
    private String etiqueta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IconoIndicadorConfianza icono;

    @Column(nullable = false)
    private boolean activo;

    @Column(nullable = false)
    private int orden;
}
