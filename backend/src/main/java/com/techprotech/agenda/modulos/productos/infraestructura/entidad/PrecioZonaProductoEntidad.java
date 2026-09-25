package com.techprotech.agenda.modulos.productos.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "producto_precio_zona")
public class PrecioZonaProductoEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;
    @Column(name = "producto_id", nullable = false)
    private Long productoId;
    @Column(name = "zona_precio_id", nullable = false)
    private Long zonaPrecioId;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;
    @Column(nullable = false, length = 10)
    private String moneda;
    @Column(nullable = false)
    private boolean activo;
}
