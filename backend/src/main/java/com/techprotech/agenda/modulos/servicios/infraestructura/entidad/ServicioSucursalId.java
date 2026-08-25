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
public class ServicioSucursalId implements Serializable {

    @Column(name = "servicio_id")
    private Long servicioId;

    @Column(name = "sucursal_id")
    private Long sucursalId;

    public ServicioSucursalId() {
    }

    public ServicioSucursalId(Long servicioId, Long sucursalId) {
        this.servicioId = servicioId;
        this.sucursalId = sucursalId;
    }

  @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServicioSucursalId that)) return false;
        return Objects.equals(servicioId, that.servicioId) && Objects.equals(sucursalId, that.sucursalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(servicioId, sucursalId);
    }
}
