package com.techprotech.agenda.modulos.plataforma.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;

@Getter @Setter @Entity
@Table(name="plataforma_plan_limite")
@IdClass(PlanLimitePlataformaEntidad.Clave.class)
public class PlanLimitePlataformaEntidad {
    @Id @Column(name="plan_id") private Long planId;
    @Id @Column(name="limite_id") private Long limiteId;
    @Column(nullable=false) private Long valor;
    public record Clave(Long planId,Long limiteId) implements Serializable {}
}
