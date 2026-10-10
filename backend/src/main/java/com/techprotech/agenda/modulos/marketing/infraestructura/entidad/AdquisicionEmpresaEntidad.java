package com.techprotech.agenda.modulos.marketing.infraestructura.entidad;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
@Getter @Setter @Entity @Table(name="empresa_adquisicion")
public class AdquisicionEmpresaEntidad {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true) private Long empresaId;
 @Column(length=80) private String source;
 @Column(length=80) private String medium;
 @Column(length=80) private String campaign;
 @Column(length=80) private String content;
 @Column(length=80) private String term;
 @Column(length=80) private String landingVertical;
 @Column(length=81) private String firstLandingPath;
 @Column(nullable=false) private Instant acquiredAt;
}
