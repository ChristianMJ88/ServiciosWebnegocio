package com.techprotech.agenda.modulos.productos.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "producto_existencia")
public class ProductoExistenciaEntidad {
    @EmbeddedId
    private ProductoExistenciaId id;
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal existencia;
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal reservado;
    @Column(name = "stock_minimo", precision = 14, scale = 3)
    private BigDecimal stockMinimo;
    @Column(name = "stock_maximo", precision = 14, scale = 3)
    private BigDecimal stockMaximo;
    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;
}
