package com.techprotech.agenda.modulos.autenticacion.social;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "codigo_acceso_social")
public class CodigoAccesoSocialEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 30) private String proveedor;
    @Column(name = "subject_proveedor", nullable = false, length = 255) private String subjectProveedor;
    @Column(name = "token_hash", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    private String tokenHash;
    @Column(name = "expira_en", nullable = false) private LocalDateTime expiraEn;
    @Column(name = "usado_en") private LocalDateTime usadoEn;
    @Column(name = "creado_en", nullable = false) private LocalDateTime creadoEn;
}
