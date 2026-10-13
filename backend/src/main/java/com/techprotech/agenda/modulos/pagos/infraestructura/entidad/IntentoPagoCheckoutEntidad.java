package com.techprotech.agenda.modulos.pagos.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "intento_pago_checkout")
public class IntentoPagoCheckoutEntidad {
    @Column(name="pago_expira_en") private java.time.LocalDateTime pagoExpiraEn;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "checkout_orden_id", nullable = false)
    private Long checkoutOrdenId;
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;
    @Column(name = "proveedor_pago_id", nullable = false)
    private Long proveedorPagoId;
    @Column(name = "metodo_pago_id", length = 80)
    private String metodoPagoId;
    @Column(name = "tipo_pago_id", length = 40)
    private String tipoPagoId;
    @Column(nullable = false, length = 128)
    private String idempotencia;
    @Column(name = "referencia_externa", length = 180)
    private String referenciaExterna;
    @Column(name = "referencia_pago_externa", length = 180)
    private String referenciaPagoExterna;
    @Column(nullable = false, length = 30)
    private String estado;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importe;
    @Column(nullable = false, length = 10)
    private String moneda;
    @Column(name = "accion_url", length = 1000)
    private String accionUrl;
    @Column(name = "codigo_respuesta", length = 120)
    private String codigoRespuesta;
    @Column(name = "detalle_respuesta", length = 500)
    private String detalleRespuesta;
    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;
    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;
}
