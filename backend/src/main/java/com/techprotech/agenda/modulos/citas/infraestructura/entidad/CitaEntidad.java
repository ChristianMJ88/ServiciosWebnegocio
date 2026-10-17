package com.techprotech.agenda.modulos.citas.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Getter
@Setter
@Entity
@Table(name = "cita")
public class CitaEntidad {
    @Column(name="politica_version_id", updatable=false) private Long politicaVersionId;
    @Column(name="politica_aceptada_en", updatable=false) private java.time.LocalDateTime politicaAceptadaEn;

@Column(name="pago_expira_en") private LocalDateTime pagoExpiraEn;
@Column(name="reserva_grupo_id",length=36) private String reservaGrupoId;
@Column(name="politica_cobro",length=30) private String politicaCobro;
@Column(name="importe_requerido",precision=10,scale=2) private BigDecimal importeRequerido;

  @Column(name = "folio", length = 16, updatable = false)
  private String folio;

  @jakarta.persistence.PrePersist
  void asignarFolio() {
    if (folio == null || folio.isBlank()) folio = com.techprotech.agenda.modulos.citas.dominio.FolioCita.nuevo();
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "empresa_id", nullable = false)
  private Long empresaId;

  @Column(name = "sucursal_id", nullable = false)
  private Long sucursalId;

  @Column(name = "servicio_id", nullable = false)
  private Long servicioId;

  @Column(name = "prestador_id", nullable = false)
  private Long prestadorId;

  @Column(name = "cliente_id", nullable = false)
  private Long clienteId;

  @Column(nullable = false, length = 90)
  private String estado;

  @Column(nullable = false , length = 90)
  private LocalDateTime inicio;

  @Column(nullable = false , length = 90)
  private LocalDateTime fin;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal precio;

  @Column(name = "precio_lista", precision = 10, scale = 2)
  private BigDecimal precioLista;
  @Column(precision = 10, scale = 2, nullable = false)
  private BigDecimal descuento = BigDecimal.ZERO;
  @Column(name = "promocion_id")
  private Long promocionId;
  @Column(name = "promocion_snapshot_json", columnDefinition = "json")
  private String promocionSnapshotJson;

  @Column(nullable = false, length = 10)
  private String moneda;

  @Column(length = 500)
  private String notas;

  @Column(name = "creada_por_usuario_id", nullable = false)
  private Long creadaPorUsuarioId;

  @Column(name = "creada_en", updatable = false)
  @jakarta.persistence.Convert(converter = com.techprotech.agenda.compartido.tiempo.FechaUtcPersistencia.class)
  private LocalDateTime creadaEn;

  @Column(name = "cancelada_en")
  private LocalDateTime canceladaEn;

  @Column(name = "motivo_cancelacion", length = 90)
  private String motivoCancelacion;

  @Column(name = "reprogramada_desde_id")
  private Long reprogramadaDesdeId;

  @Column(name = "check_in_en")
  private LocalDateTime checkInEn;

  @Column(name = "check_in_por_usuario_id")
  private Long checkInPorUsuarioId;

}
