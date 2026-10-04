package com.techprotech.agenda.modulos.plataforma.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @Entity
@Table(name="plataforma_plan")
public class PlanPlataformaEntidad {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true, length=80) private String codigo;
    @Column(nullable=false, length=120) private String nombre;
    @Column(length=500) private String descripcion;
    @Column(nullable=false, length=10) private String moneda;
    @Column(name="precio_mensual", nullable=false, precision=12, scale=2) private BigDecimal precioMensual;
    @Column(name="precio_anual", precision=12, scale=2) private BigDecimal precioAnual;
    @Column(nullable=false) private boolean activo;
    @Column(nullable=false) private boolean publico;
    @Column(name="creado_en", insertable=false, updatable=false) private LocalDateTime creadoEn;
    @Column(name="actualizado_en", insertable=false, updatable=false) private LocalDateTime actualizadoEn;
}
