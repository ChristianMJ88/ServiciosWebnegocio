package com.techprotech.agenda.modulos.admin.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "auditoria_rol_empresa")
public class AuditoriaRolEmpresaEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "rol_empresa_id")
    private Long rolEmpresaId;

    @Column(name = "usuario_actor_id")
    private Long usuarioActorId;

    @Column(name = "actor_correo", length = 150)
    private String actorCorreo;

    @Column(name = "rol_codigo", length = 80)
    private String rolCodigo;

    @Column(name = "rol_nombre", length = 120)
    private String rolNombre;

    @Column(nullable = false, length = 50)
    private String accion;

    @Column(nullable = false, length = 255)
    private String resumen;

    @Lob
    @Column(name = "detalle_antes_json", columnDefinition = "LONGTEXT")
    private String detalleAntesJson;

    @Lob
    @Column(name = "detalle_despues_json", columnDefinition = "LONGTEXT")
    private String detalleDespuesJson;

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime creadoEn;

}
