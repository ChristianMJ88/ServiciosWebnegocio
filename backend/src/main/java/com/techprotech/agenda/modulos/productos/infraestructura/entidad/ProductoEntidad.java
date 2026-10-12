package com.techprotech.agenda.modulos.productos.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "producto")
public class ProductoEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;
    @Column(name = "categoria_id")
    private Long categoriaId;
    @Column(name = "proveedor_id")
    private Long proveedorId;
    @Column(name = "media_id")
    private Long mediaId;
    @Column(nullable = false, length = 80)
    private String sku;
    @Column(name = "codigo_barras", length = 80)
    private String codigoBarras;
    @Column(nullable = false, length = 180)
    private String nombre;
    @Column(nullable = false, length = 200)
    private String slug;
    @Column(length = 1500)
    private String descripcion;
    @Column(name = "imagen_url", length = 1000)
    private String imagenUrl;
    @Column(name = "precio_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioBase;
    @Column(precision = 12, scale = 2)
    private BigDecimal costo;
    @Column(name = "peso_kg", precision = 10, scale = 3)
    private BigDecimal pesoKg;
    @Column(name = "largo_cm", precision = 10, scale = 2)
    private BigDecimal largoCm;
    @Column(name = "ancho_cm", precision = 10, scale = 2)
    private BigDecimal anchoCm;
    @Column(name = "alto_cm", precision = 10, scale = 2)
    private BigDecimal altoCm;
    @Column(nullable = false, length = 10)
    private String moneda;
    @Column(name = "tasa_impuesto", nullable = false, precision = 7, scale = 4)
    private BigDecimal tasaImpuesto;
    @Column(name = "controla_inventario", nullable = false)
    private boolean controlaInventario;
    @Column(name = "stock_minimo", nullable = false, precision = 14, scale = 3)
    private BigDecimal stockMinimo;
    @Column(name = "visible_publico", nullable = false)
    private boolean visiblePublico;
    @Column(nullable = false)
    private boolean activo;
    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;
    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;
    @Column(name="nuevo_desde") private java.time.LocalDate nuevoDesde;
    @Column(name="nuevo_hasta") private java.time.LocalDate nuevoHasta;
}
