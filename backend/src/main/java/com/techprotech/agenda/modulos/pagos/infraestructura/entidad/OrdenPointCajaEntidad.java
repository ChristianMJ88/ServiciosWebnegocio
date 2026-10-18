package com.techprotech.agenda.modulos.pagos.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @Entity
@Table(name = "orden_point_caja")
public class OrdenPointCajaEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "public_id", nullable = false, length = 36) private String publicId;
    @Column(name = "empresa_id", nullable = false) private Long empresaId;
    @Column(name = "venta_id", nullable = false) private Long ventaId;
    @Column(name = "terminal_id", nullable = false) private Long terminalId;
    @Column(name = "proveedor_orden_id", length = 180) private String proveedorOrdenId;
    @Column(name = "proveedor_pago_id", length = 180) private String proveedorPagoId;
    @Column(nullable = false, length = 128) private String idempotencia;
    @Column(nullable = false, length = 30) private String estado;
    @Column(name="pagos_previstos_json", columnDefinition="json", updatable=false) private String pagosPrevistosJson;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal importe;
    @Column(nullable = false, length = 10) private String moneda;
    @Column(name = "detalle_estado", length = 180) private String detalleEstado;
    @Column(name = "creado_por_usuario_id", nullable = false) private Long creadoPorUsuarioId;
    @Column(name = "creado_en", insertable = false, updatable = false) private LocalDateTime creadoEn;
    @Column(name = "actualizado_en", insertable = false, updatable = false) private LocalDateTime actualizadoEn;
}
