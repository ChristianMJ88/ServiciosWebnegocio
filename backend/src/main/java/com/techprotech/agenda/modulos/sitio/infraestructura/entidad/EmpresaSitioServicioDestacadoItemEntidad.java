package com.techprotech.agenda.modulos.sitio.infraestructura.entidad;

import com.techprotech.agenda.modulos.sitio.dominio.TipoSeleccionServicioDestacado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "empresa_sitio_servicio_destacado_item",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_sitio_destacado_referencia", columnNames = {"empresa_id", "tipo", "referencia_id"}),
                @UniqueConstraint(name = "uk_sitio_destacado_orden", columnNames = {"empresa_id", "orden"})
        }
)
public class EmpresaSitioServicioDestacadoItemEntidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoSeleccionServicioDestacado tipo;

    @Column(name = "referencia_id", nullable = false)
    private Long referenciaId;

    @Column(length = 40)
    private String etiqueta;

    @Column(nullable = false)
    private int orden;
}
