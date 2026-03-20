package com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cliente")
public class ClienteEntidad {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(nullable = false, length = 30)
    private String telefono;

    @Column(name = "acepta_whatsapp", nullable = false)
    private boolean aceptaWhatsapp;

    @Column(length = 500)
    private String notas;

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
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

    public boolean isAceptaWhatsapp() {
        return aceptaWhatsapp;
    }

    public void setAceptaWhatsapp(boolean aceptaWhatsapp) {
        this.aceptaWhatsapp = aceptaWhatsapp;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }
}

