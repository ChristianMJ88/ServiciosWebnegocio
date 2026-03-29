package com.techprotech.agenda.modulos.admin.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

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

    public Long getId() {
        return id;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public Long getRolEmpresaId() {
        return rolEmpresaId;
    }

    public void setRolEmpresaId(Long rolEmpresaId) {
        this.rolEmpresaId = rolEmpresaId;
    }

    public Long getUsuarioActorId() {
        return usuarioActorId;
    }

    public void setUsuarioActorId(Long usuarioActorId) {
        this.usuarioActorId = usuarioActorId;
    }

    public String getActorCorreo() {
        return actorCorreo;
    }

    public void setActorCorreo(String actorCorreo) {
        this.actorCorreo = actorCorreo;
    }

    public String getRolCodigo() {
        return rolCodigo;
    }

    public void setRolCodigo(String rolCodigo) {
        this.rolCodigo = rolCodigo;
    }

    public String getRolNombre() {
        return rolNombre;
    }

    public void setRolNombre(String rolNombre) {
        this.rolNombre = rolNombre;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getResumen() {
        return resumen;
    }

    public void setResumen(String resumen) {
        this.resumen = resumen;
    }

    public String getDetalleAntesJson() {
        return detalleAntesJson;
    }

    public void setDetalleAntesJson(String detalleAntesJson) {
        this.detalleAntesJson = detalleAntesJson;
    }

    public String getDetalleDespuesJson() {
        return detalleDespuesJson;
    }

    public void setDetalleDespuesJson(String detalleDespuesJson) {
        this.detalleDespuesJson = detalleDespuesJson;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }
}
