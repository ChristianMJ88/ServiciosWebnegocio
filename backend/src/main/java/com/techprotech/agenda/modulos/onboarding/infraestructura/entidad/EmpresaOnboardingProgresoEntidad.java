package com.techprotech.agenda.modulos.onboarding.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "empresa_onboarding_progreso")
public class EmpresaOnboardingProgresoEntidad {
    @Id
    @Column(name = "empresa_id")
    private Long empresaId;

    @Column(nullable = false, length = 120)
    private String categoria;

    @Column(name = "tamano_equipo", nullable = false, length = 30)
    private String tamanoEquipo;

    @Column(name = "paso_recomendado", nullable = false, length = 40)
    private String pasoRecomendado;

    @Column(name = "horario_completado", nullable = false)
    private boolean horarioCompletado;

    @Column(name = "servicio_completado", nullable = false)
    private boolean servicioCompletado;

    @Column(name = "personal_completado", nullable = false)
    private boolean personalCompletado;

    @Column(name = "sitio_completado", nullable = false)
    private boolean sitioCompletado;

    @Column(name = "cita_prueba_completada", nullable = false)
    private boolean citaPruebaCompletada;

    @Column(nullable = false)
    private boolean omitido;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;
}
