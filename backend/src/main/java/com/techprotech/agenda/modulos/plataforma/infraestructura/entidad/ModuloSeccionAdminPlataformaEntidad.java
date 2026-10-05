package com.techprotech.agenda.modulos.plataforma.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Entity
@Table(name="plataforma_modulo_seccion_admin")
@IdClass(ModuloSeccionAdminPlataformaEntidad.Clave.class)
public class ModuloSeccionAdminPlataformaEntidad {
    @Id @Column(name="modulo_id") private Long moduloId;
    @Id @Column(name="seccion_codigo",length=80) private String seccionCodigo;
    public record Clave(Long moduloId,String seccionCodigo) implements java.io.Serializable {}
}
