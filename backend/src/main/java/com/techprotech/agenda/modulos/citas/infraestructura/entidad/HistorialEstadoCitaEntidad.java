package com.techprotech.agenda.modulos.citas.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "historial_estado_cita")
public class HistorialEstadoCitaEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cita_id", nullable = false)
    private Long citaId;

    @Column(name = "estado_anterior", length = 20)
    private String estadoAnterior;

    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private String estadoNuevo;

    @Column(name = "cambiado_por_usuario_id", nullable = false)
    private Long cambiadoPorUsuarioId;

    @Column(length = 255)
    private String motivo;

    public void setCitaId(Long citaId) {
        this.citaId = citaId;
    }

    public void setEstadoAnterior(String estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public void setEstadoNuevo(String estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public void setCambiadoPorUsuarioId(Long cambiadoPorUsuarioId) {
        this.cambiadoPorUsuarioId = cambiadoPorUsuarioId;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}

