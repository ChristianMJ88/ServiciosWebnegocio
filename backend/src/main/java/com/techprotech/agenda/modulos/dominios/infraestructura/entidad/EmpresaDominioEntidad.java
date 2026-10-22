package com.techprotech.agenda.modulos.dominios.infraestructura.entidad;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import com.techprotech.agenda.compartido.tiempo.FechaUtcPersistencia;
@Getter @Setter @Entity @Table(name="empresa_dominio")
public class EmpresaDominioEntidad {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long empresaId;
 @Column(nullable=false,length=253) private String hostname;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private EstadoDominio estado;
 @Column(nullable=false,length=43) private String tokenVerificacion;
 @Column(columnDefinition="json") private String registrosHosting;
 @Column(length=64) private String identificadorBuild;
 @Convert(converter=FechaUtcPersistencia.class) private LocalDateTime verificadoEn;
 @Convert(converter=FechaUtcPersistencia.class) private LocalDateTime activadoEn;
 @Convert(converter=FechaUtcPersistencia.class) private LocalDateTime ultimoChequeoEn;
 @Column(nullable=false) private boolean chequeoEnCurso;
 @Convert(converter=FechaUtcPersistencia.class) private LocalDateTime proximoChequeoEn;
 @Column(nullable=false) private int intentosChequeo;
 @Column(length=180) private String ultimoError;
 @Convert(converter=FechaUtcPersistencia.class) @Column(nullable=false) private LocalDateTime creadoEn;
 private Long creadoPor;
 @Version private Long version;
}
