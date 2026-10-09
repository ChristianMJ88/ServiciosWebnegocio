package com.techprotech.agenda.modulos.marketing.infraestructura.entidad;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter @Entity @Table(name="plataforma_landing")
public class LandingMarketingEntidad {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=80) private String slug;
 @Column(nullable=false) private boolean publicado;
 @Column(nullable=false,columnDefinition="json") private String contenido;
 @Version private Long version;
}
