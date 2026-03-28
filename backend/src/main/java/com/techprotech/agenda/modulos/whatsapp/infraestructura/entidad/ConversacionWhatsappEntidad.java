package com.techprotech.agenda.modulos.whatsapp.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "whatsapp_conversacion",
        uniqueConstraints = @UniqueConstraint(name = "uk_whatsapp_conversacion_empresa_telefono", columnNames = {"empresa_id", "telefono_normalizado"})
)
public class ConversacionWhatsappEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "telefono_normalizado", nullable = false, length = 30)
    private String telefonoNormalizado;

    @Column(name = "flujo", nullable = false, length = 40)
    private String flujo;

    @Column(name = "paso", nullable = false, length = 40)
    private String paso;

    @Column(name = "sucursal_id")
    private Long sucursalId;

    @Column(name = "servicio_id")
    private Long servicioId;

    @Column(name = "cita_id")
    private Long citaId;

    @Column(name = "fecha_seleccionada")
    private LocalDate fechaSeleccionada;

    @Column(name = "hora_seleccionada", length = 10)
    private String horaSeleccionada;

    @Column(name = "nombre_cliente", length = 150)
    private String nombreCliente;

    @Column(name = "correo_cliente", length = 150)
    private String correoCliente;

    @Column(name = "actualizada_en", nullable = false)
    private LocalDateTime actualizadaEn;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public String getTelefonoNormalizado() {
        return telefonoNormalizado;
    }

    public void setTelefonoNormalizado(String telefonoNormalizado) {
        this.telefonoNormalizado = telefonoNormalizado;
    }

    public String getFlujo() {
        return flujo;
    }

    public void setFlujo(String flujo) {
        this.flujo = flujo;
    }

    public String getPaso() {
        return paso;
    }

    public void setPaso(String paso) {
        this.paso = paso;
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

    public Long getCitaId() {
        return citaId;
    }

    public void setCitaId(Long citaId) {
        this.citaId = citaId;
    }

    public LocalDate getFechaSeleccionada() {
        return fechaSeleccionada;
    }

    public void setFechaSeleccionada(LocalDate fechaSeleccionada) {
        this.fechaSeleccionada = fechaSeleccionada;
    }

    public String getHoraSeleccionada() {
        return horaSeleccionada;
    }

    public void setHoraSeleccionada(String horaSeleccionada) {
        this.horaSeleccionada = horaSeleccionada;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getCorreoCliente() {
        return correoCliente;
    }

    public void setCorreoCliente(String correoCliente) {
        this.correoCliente = correoCliente;
    }

    public LocalDateTime getActualizadaEn() {
        return actualizadaEn;
    }

    public void setActualizadaEn(LocalDateTime actualizadaEn) {
        this.actualizadaEn = actualizadaEn;
    }
}
