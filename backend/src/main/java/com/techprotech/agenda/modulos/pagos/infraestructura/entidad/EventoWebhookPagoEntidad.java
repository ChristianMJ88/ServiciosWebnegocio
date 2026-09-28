package com.techprotech.agenda.modulos.pagos.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "evento_webhook_pago")
public class EventoWebhookPagoEntidad {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "proveedor_pago_id", nullable = false)
  private Long proveedorPagoId;
  @Column(name = "empresa_id")
  private Long empresaId;
  @Column(name = "evento_externo_id", nullable = false, length = 180)
  private String eventoExternoId;
  @Column(nullable = false, length = 100)
  private String tipo;
  @Column(name = "modo_produccion", nullable = false)
  private boolean modoProduccion;
  @Column(name = "estado_procesamiento", nullable = false, length = 30)
  private String estadoProcesamiento;
  @Column(nullable = false)
  private int intentos;
  @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
  private String payload;
  @Column(name = "error_ultimo", length = 1000)
  private String errorUltimo;
  @Column(name = "recibido_en", insertable = false, updatable = false)
  private LocalDateTime recibidoEn;
  @Column(name = "procesado_en")
  private LocalDateTime procesadoEn;
}
