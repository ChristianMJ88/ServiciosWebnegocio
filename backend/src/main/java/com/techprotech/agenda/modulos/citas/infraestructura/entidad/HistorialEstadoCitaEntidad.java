package com.techprotech.agenda.modulos.citas.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "historial_estado_cita")
public class HistorialEstadoCitaEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cita_id", nullable = false)
    private Long citaId;

    @Column(name = "estado_anterior", length = 80)
    private String estadoAnterior;

    @Column(name = "estado_nuevo", nullable = false, length = 80)
    private String estadoNuevo;

    @Column(name = "cambiado_por_usuario_id", nullable = false)
    private Long cambiadoPorUsuarioId;

    @Column(length = 90)
    private String motivo;
}

