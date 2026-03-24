package com.techprotech.agenda.modulos.contactos.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "solicitud_contacto")
public class SolicitudContactoEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(length = 30)
    private String telefono;

    @Column(nullable = false, length = 150)
    private String correo;

    @Column(nullable = false, length = 180)
    private String asunto;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Column(nullable = false, length = 30)
    private String canal;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "notificacion_correo_programada", nullable = false)
    private boolean notificacionCorreoProgramada;

    @Column(name = "notificada_en")
    private LocalDateTime notificadaEn;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private LocalDateTime creadaEn;

    @PrePersist
    void inicializarCamposAutomaticos() {
        if (creadaEn == null) {
            creadaEn = LocalDateTime.now();
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

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getCanal() {
        return canal;
    }

    public void setCanal(String canal) {
        this.canal = canal;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public boolean isNotificacionCorreoProgramada() {
        return notificacionCorreoProgramada;
    }

    public void setNotificacionCorreoProgramada(boolean notificacionCorreoProgramada) {
        this.notificacionCorreoProgramada = notificacionCorreoProgramada;
    }

    public LocalDateTime getNotificadaEn() {
        return notificadaEn;
    }

    public void setNotificadaEn(LocalDateTime notificadaEn) {
        this.notificadaEn = notificadaEn;
    }

    public LocalDateTime getCreadaEn() {
        return creadaEn;
    }
}
