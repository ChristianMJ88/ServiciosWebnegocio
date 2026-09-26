package com.techprotech.agenda.modulos.caja.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @Entity
@Table(name = "venta")
public class VentaEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "empresa_id", nullable = false) private Long empresaId;
    @Column(name = "sucursal_id", nullable = false) private Long sucursalId;
    @Column(name = "caja_sesion_id", nullable = false) private Long cajaSesionId;
    @Column(name = "cliente_id") private Long clienteId;
    @Column(name = "cita_id") private Long citaId;
    @Column(nullable = false, length = 40) private String folio;
    @Column(nullable = false, length = 20) private String estado;
    @Column(nullable = false, length = 10) private String moneda;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal subtotal;
    @Column(name = "descuento_total", nullable = false, precision = 12, scale = 2) private BigDecimal descuentoTotal;
    @Column(name = "impuestos_total", nullable = false, precision = 12, scale = 2) private BigDecimal impuestosTotal;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal total;
    @Column(length = 500) private String observaciones;
    @Column(name = "creada_por_usuario_id", nullable = false) private Long creadaPorUsuarioId;
    @Column(name = "creada_en", nullable = false) private LocalDateTime creadaEn;
}
