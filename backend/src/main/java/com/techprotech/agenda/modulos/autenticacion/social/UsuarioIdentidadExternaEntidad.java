package com.techprotech.agenda.modulos.autenticacion.social;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "usuario_identidad_externa")
public class UsuarioIdentidadExternaEntidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 30)
    private String proveedor;

    @Column(name = "subject_proveedor", nullable = false, length = 255)
    private String subjectProveedor;

    @Column(name = "correo_verificado", nullable = false, length = 150)
    private String correoVerificado;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;
}
