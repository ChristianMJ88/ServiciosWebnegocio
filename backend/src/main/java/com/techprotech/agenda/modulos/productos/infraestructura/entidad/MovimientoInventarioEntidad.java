package com.techprotech.agenda.modulos.productos.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "producto_movimiento_inventario")
public class MovimientoInventarioEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;
    @Column(name = "producto_id", nullable = false)
    private Long productoId;
    @Column(name = "sucursal_id", nullable = false)
    private Long sucursalId;
    @Column(nullable = false, length = 30)
    private String tipo;
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidad;
    @Column(name = "existencia_anterior", nullable = false, precision = 14, scale = 3)
    private BigDecimal existenciaAnterior;
    @Column(name = "existencia_nueva", nullable = false, precision = 14, scale = 3)
    private BigDecimal existenciaNueva;
    @Column(length = 120)
    private String referencia;
    @Column(length = 500)
    private String notas;
    @Column(name = "creado_por_usuario_id", nullable = false)
    private Long creadoPorUsuarioId;
    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;
}
