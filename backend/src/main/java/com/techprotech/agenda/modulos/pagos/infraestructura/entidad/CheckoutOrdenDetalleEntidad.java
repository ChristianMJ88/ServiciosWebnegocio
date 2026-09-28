package com.techprotech.agenda.modulos.pagos.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "checkout_orden_detalle")
public class CheckoutOrdenDetalleEntidad {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "checkout_orden_id", nullable = false)
  private Long checkoutOrdenId;
  @Column(name = "empresa_id", nullable = false)
  private Long empresaId;
  @Column(name = "tipo_concepto", nullable = false, length = 20)
  private String tipoConcepto;
  @Column(name = "producto_id")
  private Long productoId;
  @Column(name = "servicio_id")
  private Long servicioId;
  @Column(name = "cita_id")
  private Long citaId;
  @Column(nullable = false, length = 180)
  private String nombre;
  @Column(nullable = false, precision = 14, scale = 3)
  private BigDecimal cantidad;
  @Column(name = "precio_lista_unitario", nullable = false, precision = 12, scale = 2)
  private BigDecimal precioListaUnitario;
  @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
  private BigDecimal precioUnitario;
  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal descuento;
  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal impuestos;
  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal total;
}
