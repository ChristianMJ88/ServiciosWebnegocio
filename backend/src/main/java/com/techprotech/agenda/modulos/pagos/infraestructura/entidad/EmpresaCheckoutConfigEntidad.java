package com.techprotech.agenda.modulos.pagos.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "empresa_checkout_config")
public class EmpresaCheckoutConfigEntidad {
    @Id
    @Column(name = "empresa_id")
    private Long empresaId;
    @Column(name = "proveedor_predeterminado_id")
    private Long proveedorPredeterminadoId;
    @Column(nullable = false)
    private boolean habilitado;
    @Column(name = "aceptar_tarjeta", nullable = false)
    private boolean aceptarTarjeta = true;
    @Column(name = "aceptar_transferencia", nullable = false)
    private boolean aceptarTransferencia = true;
    @Column(name = "aceptar_efectivo_referenciado", nullable = false)
    private boolean aceptarEfectivoReferenciado;
    @Column(name = "permitir_pago_productos", nullable = false)
    private boolean permitirPagoProductos;
    @Column(name = "permitir_pago_servicios", nullable = false)
    private boolean permitirPagoServicios;
    @Column(name = "modalidad_servicios", nullable = false, length = 30)
    private String modalidadServicios = "PAGO_COMPLETO";
    @Column(name = "anticipo_porcentaje", precision = 7, scale = 4)
    private BigDecimal anticipoPorcentaje;
    @Column(name = "anticipo_importe", precision = 12, scale = 2)
    private BigDecimal anticipoImporte;
    @Column(nullable = false, length = 10)
    private String moneda = "MXN";
    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;
}
