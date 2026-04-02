package com.techprotech.agenda.modulos.recepcion.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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

    public Long getId() {
        return id;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public Long getSucursalId() {
        return sucursalId;
    }

    public void setSucursalId(Long sucursalId) {
        this.sucursalId = sucursalId;
    }

    public Long getServicioId() {
        return servicioId;
    }

    public void setServicioId(Long servicioId) {
        this.servicioId = servicioId;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public Long getCitaId() {
        return citaId;
    }

    public void setCitaId(Long citaId) {
        this.citaId = citaId;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getTelefonoCliente() {
        return telefonoCliente;
    }

    public void setTelefonoCliente(String telefonoCliente) {
        this.telefonoCliente = telefonoCliente;
    }

    public LocalDate getFechaDeseada() {
        return fechaDeseada;
    }

    public void setFechaDeseada(LocalDate fechaDeseada) {
        this.fechaDeseada = fechaDeseada;
    }

    public LocalTime getHoraDesde() {
        return horaDesde;
    }

    public void setHoraDesde(LocalTime horaDesde) {
        this.horaDesde = horaDesde;
    }

    public LocalTime getHoraHasta() {
        return horaHasta;
    }

    public void setHoraHasta(LocalTime horaHasta) {
        this.horaHasta = horaHasta;
    }

    public boolean isAceptaWhatsapp() {
        return aceptaWhatsapp;
    }

    public void setAceptaWhatsapp(boolean aceptaWhatsapp) {
        this.aceptaWhatsapp = aceptaWhatsapp;
    }

    public String getCanalOrigen() {
        return canalOrigen;
    }

    public void setCanalOrigen(String canalOrigen) {
        this.canalOrigen = canalOrigen;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public Long getCreadoPorUsuarioId() {
        return creadoPorUsuarioId;
    }

    public void setCreadoPorUsuarioId(Long creadoPorUsuarioId) {
        this.creadoPorUsuarioId = creadoPorUsuarioId;
    }

    public LocalDateTime getCreadaEn() {
        return creadaEn;
    }

    public LocalDateTime getNotificadaEn() {
        return notificadaEn;
    }

    public void setNotificadaEn(LocalDateTime notificadaEn) {
        this.notificadaEn = notificadaEn;
    }

    public LocalDateTime getCerradoEn() {
        return cerradoEn;
    }

    public void setCerradoEn(LocalDateTime cerradoEn) {
        this.cerradoEn = cerradoEn;
    }
}
