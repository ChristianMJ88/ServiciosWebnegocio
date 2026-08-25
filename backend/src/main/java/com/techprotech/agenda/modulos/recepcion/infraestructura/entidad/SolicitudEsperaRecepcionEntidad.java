package com.techprotech.agenda.modulos.recepcion.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Setter
@Getter
@Entity
@Table(name = "recepcion_solicitud_espera")
public class SolicitudEsperaRecepcionEntidad {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "empresa_id", nullable = false)
  private Long empresaId;

  @Column(name = "sucursal_id", nullable = false)
  private Long sucursalId;

  @Column(name = "servicio_id", nullable = false)
  private Long servicioId;

  @Column(name = "cliente_id")
  private Long clienteId;

  @Column(name = "cita_id")
  private Long citaId;

  @Column(name = "nombre_cliente", nullable = false, length = 150)
  private String nombreCliente;

  @Column(name = "telefono_cliente", nullable = false, length = 30)
  private String telefonoCliente;

  @Column(name = "fecha_deseada", nullable = false)
  private LocalDate fechaDeseada;

  @Column(name = "hora_desde")
  private LocalTime horaDesde;

  @Column(name = "hora_hasta")
  private LocalTime horaHasta;

  @Column(name = "acepta_whatsapp", nullable = false)
  private boolean aceptaWhatsapp;

  @Column(name = "canal_origen", nullable = false, length = 30)
  private String canalOrigen;

  @Column(nullable = false, length = 20)
  private String estado;

  @Column(length = 500)
  private String notas;

  @Column(name = "creado_por_usuario_id")
  private Long creadoPorUsuarioId;

  @Column(name = "creada_en", nullable = false, updatable = false)
  private LocalDateTime creadaEn;

  @Column(name = "notificada_en")
  private LocalDateTime notificadaEn;

  @Column(name = "cerrado_en")
  private LocalDateTime cerradoEn;

  @PrePersist
  void inicializarCamposAutomaticos() {
    if (creadaEn == null) {
      creadaEn = LocalDateTime.now();
    }
    if (estado == null || estado.isBlank()) {
      estado = "PENDIENTE";
    }
    if (canalOrigen == null || canalOrigen.isBlank()) {
      canalOrigen = "MOSTRADOR";
    }
  }

}
