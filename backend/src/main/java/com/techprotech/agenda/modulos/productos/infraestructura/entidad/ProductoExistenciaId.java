package com.techprotech.agenda.modulos.productos.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ProductoExistenciaId implements Serializable {
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;
    @Column(name = "producto_id", nullable = false)
    private Long productoId;
    @Column(name = "sucursal_id", nullable = false)
    private Long sucursalId;
}
