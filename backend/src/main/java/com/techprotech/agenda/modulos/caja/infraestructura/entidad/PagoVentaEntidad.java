package com.techprotech.agenda.modulos.caja.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @Entity
@Table(name = "pago_venta")
public class PagoVentaEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "empresa_id", nullable = false) private Long empresaId;
    @Column(name = "venta_id", nullable = false) private Long ventaId;
    @Column(name = "intento_pago_checkout_id") private Long intentoPagoCheckoutId;
    @Column(name = "caja_sesion_id") private Long cajaSesionId;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal monto;
    @Column(name = "metodo_pago", nullable = false, length = 30) private String metodoPago;
    @Column(length = 120) private String referencia;
    @Column(name = "registrado_por_usuario_id") private Long registradoPorUsuarioId;
    @Column(name = "registrado_en", nullable = false) private LocalDateTime registradoEn;
}
