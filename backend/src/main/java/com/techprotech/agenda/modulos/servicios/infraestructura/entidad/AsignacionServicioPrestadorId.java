package com.techprotech.agenda.modulos.servicios.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;
@Setter
@Getter
@Embeddable
public class AsignacionServicioPrestadorId implements Serializable {

    @Column(name = "prestador_id")
    private Long prestadorId;

    @Column(name = "servicio_id")
    private Long servicioId;

    public AsignacionServicioPrestadorId() {
    }

    public AsignacionServicioPrestadorId(Long prestadorId, Long servicioId) {
        this.prestadorId = prestadorId;
        this.servicioId = servicioId;
    }

  @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AsignacionServicioPrestadorId that)) return false;
        return Objects.equals(prestadorId, that.prestadorId) && Objects.equals(servicioId, that.servicioId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prestadorId, servicioId);
    }
}

