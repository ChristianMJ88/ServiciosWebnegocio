package com.techprotech.agenda.modulos.plataforma.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Entity
@Table(name="plataforma_plan_modulo")
@IdClass(PlanModuloPlataformaEntidad.Clave.class)
public class PlanModuloPlataformaEntidad {
    @Id @Column(name="plan_id") private Long planId;
    @Id @Column(name="modulo_id") private Long moduloId;
    @Column(name="limite_uso") private Long limiteUso;
    public record Clave(Long planId, Long moduloId) implements java.io.Serializable {}
}
