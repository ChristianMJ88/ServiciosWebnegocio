package com.techprotech.agenda.modulos.disponibilidad.infraestructura.entidad;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Getter @Setter @Entity @Table(name="solicitud_horario_staff")
public class SolicitudHorarioEntidad {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private Long empresaId;
 private Long prestadorId;
 private Long reglaId;
 @Column(columnDefinition="LONGTEXT") private String propuesta;
 private String estado;
 private LocalDateTime creadaEn;
 private LocalDateTime resueltaEn;
 private Long resueltaPor;
}
