package com.techprotech.agenda.modulos.plataforma.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Entity
@Table(name="plataforma_seccion_admin")
public class SeccionAdminPlataformaEntidad {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,unique=true,length=80) private String codigo;
    @Column(nullable=false,length=120) private String nombre;
    @Column(length=255) private String descripcion;
    @Column(name="requiere_modulo",nullable=false) private boolean requiereModulo;
    @Column(nullable=false) private boolean activo;
    @Column(nullable=false) private int orden;
}
