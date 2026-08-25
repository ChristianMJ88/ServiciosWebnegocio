package com.techprotech.agenda.modulos.servicios.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
@Setter
@Getter
@Entity
@Table(name = "asignacion_servicio_prestador")
public class AsignacionServicioPrestadorEntidad {

    @EmbeddedId
    private AsignacionServicioPrestadorId id;

    @Column(name = "duracion_personalizada_minutos")
    private Integer duracionPersonalizadaMinutos;

    @Column(name = "precio_personalizado", precision = 10, scale = 2)
    private BigDecimal precioPersonalizado;

    @Column(nullable = false)
    private boolean activa;

}
