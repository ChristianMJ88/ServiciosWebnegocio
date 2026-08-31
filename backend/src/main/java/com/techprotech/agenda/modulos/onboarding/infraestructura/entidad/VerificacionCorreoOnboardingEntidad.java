package com.techprotech.agenda.modulos.onboarding.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @Entity @Table(name = "verificacion_correo_onboarding")
public class VerificacionCorreoOnboardingEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="empresa_id", nullable=false) private Long empresaId;
    @Column(name="usuario_id", nullable=false) private Long usuarioId;
    @Column(nullable=false, length=190) private String correo;
    @Column(name="token_hash", nullable=false, unique=true, length=64) private String tokenHash;
    @Column(nullable=false, length=20) private String estado;
    @Column(name="expira_en", nullable=false) private LocalDateTime expiraEn;
    @Column(name="confirmado_en") private LocalDateTime confirmadoEn;
    @Column(name="creado_en", nullable=false) private LocalDateTime creadoEn;
}
