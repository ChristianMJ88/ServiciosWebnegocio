package com.techprotech.agenda.modulos.marketing.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;

@Getter @Setter @Entity @Table(name="plataforma_contacto")
public class ContactoPlataformaEntidad {
    @Id private Byte id;
    @Column(nullable=false,length=150) private String correo;
    @Column(nullable=false,length=150) private String correoLegal;
    @Column(nullable=false,length=16) private String telefono;
    @Column(nullable=false,columnDefinition="json") private String dias;
    @Column(nullable=false) private LocalTime horaInicio;
    @Column(nullable=false) private LocalTime horaFin;
    @Column(nullable=false,length=64) private String zonaHoraria;
    @Column(nullable=false) private boolean visible;
    @Convert(converter=com.techprotech.agenda.compartido.tiempo.FechaUtcPersistencia.class)
    @Column(nullable=false) private LocalDateTime actualizadoEn;
    private Long actualizadoPor;
    @Version private Long version;
}
