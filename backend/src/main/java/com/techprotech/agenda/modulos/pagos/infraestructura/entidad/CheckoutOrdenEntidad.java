package com.techprotech.agenda.modulos.pagos.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @Entity
@Table(name = "checkout_orden")
public class CheckoutOrdenEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "public_id", nullable = false, unique = true, length = 36) private String publicId;
    @Column(name = "empresa_id", nullable = false) private Long empresaId;
    @Column(name = "sucursal_id", nullable = false) private Long sucursalId;
    @Column(name = "zona_precio_id") private Long zonaPrecioId;
    @Column(nullable = false, length = 128) private String idempotencia;
    @Column(nullable = false, length = 30) private String estado;
    @Column(name = "correo_cliente", nullable = false, length = 180) private String correoCliente;
    @Column(nullable = false, length = 10) private String moneda;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal subtotal;
    @Column(name = "descuento_total", nullable = false, precision = 12, scale = 2) private BigDecimal descuentoTotal;
    @Column(name = "impuestos_total", nullable = false, precision = 12, scale = 2) private BigDecimal impuestosTotal;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal total;
    @Column(name = "expira_en", nullable = false) private LocalDateTime expiraEn;
    @Column(name = "creada_en", insertable = false, updatable = false) private LocalDateTime creadaEn;
    @Column(name = "actualizada_en", insertable = false, updatable = false) private LocalDateTime actualizadaEn;
}
