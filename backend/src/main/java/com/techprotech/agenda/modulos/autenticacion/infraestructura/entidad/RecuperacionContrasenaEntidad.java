package com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @Entity @Table(name = "recuperacion_contrasena")
public class RecuperacionContrasenaEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="usuario_id", nullable=false) private Long usuarioId;
    @Column(name="empresa_id", nullable=false) private Long empresaId;
    @Column(name="token_hash", nullable=false, unique=true, length=64) private String tokenHash;
    @Column(nullable=false, length=20) private String estado;
    @Column(name="expira_en", nullable=false) private LocalDateTime expiraEn;
    @Column(name="utilizada_en") private LocalDateTime utilizadaEn;
    @Column(name="creado_en", nullable=false) private LocalDateTime creadoEn;
}
