package com.techprotech.agenda.modulos.pagos.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "conciliacion_pago")
public class ConciliacionPagoEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "empresa_id", nullable = false) private Long empresaId;
    @Column(name = "proveedor_pago_id", nullable = false) private Long proveedorPagoId;
    @Column(nullable = false, length = 20) private String canal;
    @Column(name = "checkout_orden_id") private Long checkoutOrdenId;
    @Column(name = "intento_pago_checkout_id") private Long intentoPagoCheckoutId;
    @Column(name = "orden_point_caja_id") private Long ordenPointCajaId;
    @Column(name = "venta_id") private Long ventaId;
    @Column(name = "referencia_orden_externa", nullable = false, length = 180) private String referenciaOrdenExterna;
    @Column(name = "referencia_pago_externa", length = 180) private String referenciaPagoExterna;
    @Column(nullable = false, length = 30) private String estado;
    @Column(nullable = false, length = 10) private String moneda;
    @Column(name = "importe_bruto", nullable = false, precision = 14, scale = 2) private BigDecimal importeBruto;
    @Column(name = "comision_proveedor_total", precision = 14, scale = 2) private BigDecimal comisionProveedorTotal;
    @Column(name = "impuesto_comision_proveedor", precision = 14, scale = 2) private BigDecimal impuestoComisionProveedor;
    @Column(name = "comision_plataforma", precision = 14, scale = 2) private BigDecimal comisionPlataforma;
    @Column(name = "impuesto_comision_plataforma", precision = 14, scale = 2) private BigDecimal impuestoComisionPlataforma;
    @Column(precision = 14, scale = 2) private BigDecimal retenciones;
    @Column(name = "importe_reembolsado", nullable = false, precision = 14, scale = 2) private BigDecimal importeReembolsado;
    @Column(name = "importe_neto", precision = 14, scale = 2) private BigDecimal importeNeto;
    @Column(nullable = false, length = 30) private String fuente;
    @Column(name = "liquidacion_definitiva", nullable = false) private boolean liquidacionDefinitiva;
    @Column(name = "liquidado_en") private LocalDateTime liquidadoEn;
    @Column(name = "creado_en", insertable = false, updatable = false) private LocalDateTime creadoEn;
    @Column(name = "actualizado_en", insertable = false, updatable = false) private LocalDateTime actualizadoEn;
}
