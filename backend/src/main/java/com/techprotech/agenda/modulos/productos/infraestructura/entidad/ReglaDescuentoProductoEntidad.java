package com.techprotech.agenda.modulos.productos.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @Entity
@Table(name = "producto_regla_descuento")
public class ReglaDescuentoProductoEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="empresa_id", nullable=false) private Long empresaId;
    @Column(nullable=false, length=160) private String nombre;
    @Column(length=500) private String descripcion;
    @Column(nullable=false, length=20) private String alcance;
    @Column(name="producto_id") private Long productoId;
    @Column(name="categoria_id") private Long categoriaId;
    @Column(name="zona_precio_id") private Long zonaPrecioId;
    @Column(name="sucursal_id") private Long sucursalId;
    @Column(name="tipo_descuento", nullable=false, length=20) private String tipoDescuento;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal valor;
    @Column(length=10) private String moneda;
    @Column(name="cantidad_minima", nullable=false, precision=14, scale=3) private BigDecimal cantidadMinima;
    @Column(name="fecha_inicio") private LocalDate fechaInicio;
    @Column(name="fecha_fin") private LocalDate fechaFin;
    @Column(nullable=false) private int prioridad;
    @Column(nullable=false) private boolean combinable;
    @Column(nullable=false) private boolean activa;
    @Column(name="creado_en", insertable=false, updatable=false) private LocalDateTime creadoEn;
    @Column(name="actualizado_en", insertable=false, updatable=false) private LocalDateTime actualizadoEn;
}
