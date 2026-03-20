package com.techprotech.agenda.modulos.servicios.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

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

    public AsignacionServicioPrestadorId getId() {
        return id;
    }

    public void setId(AsignacionServicioPrestadorId id) {
        this.id = id;
    }

    public Integer getDuracionPersonalizadaMinutos() {
        return duracionPersonalizadaMinutos;
    }

    public void setDuracionPersonalizadaMinutos(Integer duracionPersonalizadaMinutos) {
        this.duracionPersonalizadaMinutos = duracionPersonalizadaMinutos;
    }

    public BigDecimal getPrecioPersonalizado() {
        return precioPersonalizado;
    }

    public void setPrecioPersonalizado(BigDecimal precioPersonalizado) {
        this.precioPersonalizado = precioPersonalizado;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }
}
