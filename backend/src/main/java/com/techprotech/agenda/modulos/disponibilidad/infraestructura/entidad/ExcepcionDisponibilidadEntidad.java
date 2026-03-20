package com.techprotech.agenda.modulos.disponibilidad.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "excepcion_disponibilidad")
public class ExcepcionDisponibilidadEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "tipo_sujeto", nullable = false, length = 20)
    private String tipoSujeto;

    @Column(name = "sujeto_id", nullable = false)
    private Long sujetoId;

    @Column(name = "fecha_excepcion", nullable = false)
    private LocalDate fechaExcepcion;

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "hora_fin")
    private LocalTime horaFin;

    @Column(name = "tipo_bloqueo", nullable = false, length = 20)
    private String tipoBloqueo;

    @Column(length = 255)
    private String motivo;

    public Long getId() {
        return id;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public String getTipoSujeto() {
        return tipoSujeto;
    }

    public void setTipoSujeto(String tipoSujeto) {
        this.tipoSujeto = tipoSujeto;
    }

    public Long getSujetoId() {
        return sujetoId;
    }

    public void setSujetoId(Long sujetoId) {
        this.sujetoId = sujetoId;
    }

    public LocalDate getFechaExcepcion() {
        return fechaExcepcion;
    }

    public void setFechaExcepcion(LocalDate fechaExcepcion) {
        this.fechaExcepcion = fechaExcepcion;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }

    public String getTipoBloqueo() {
        return tipoBloqueo;
    }

    public void setTipoBloqueo(String tipoBloqueo) {
        this.tipoBloqueo = tipoBloqueo;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
