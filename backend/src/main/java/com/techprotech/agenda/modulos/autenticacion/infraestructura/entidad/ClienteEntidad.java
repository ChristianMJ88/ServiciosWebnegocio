package com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "cliente")
public class ClienteEntidad {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(nullable = false, length = 30)
    private String telefono;

    @Column(name = "acepta_whatsapp", nullable = false)
    private boolean aceptaWhatsapp;

    @Column(length = 500)
    private String notas;

}

