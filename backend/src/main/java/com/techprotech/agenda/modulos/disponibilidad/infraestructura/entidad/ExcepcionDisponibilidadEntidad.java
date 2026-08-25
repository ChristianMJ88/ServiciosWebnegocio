package com.techprotech.agenda.modulos.disponibilidad.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Setter
@Getter
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

    @Column(length = 90)
    private String motivo;

}
