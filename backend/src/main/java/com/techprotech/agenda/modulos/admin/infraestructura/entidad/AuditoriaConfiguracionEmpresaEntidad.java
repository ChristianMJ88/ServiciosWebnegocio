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
@Table(name = "auditoria_configuracion_empresa")
public class AuditoriaConfiguracionEmpresaEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "usuario_actor_id")
    private Long usuarioActorId;

    @Column(name = "actor_correo", length = 150)
    private String actorCorreo;

    @Column(nullable = false, length = 30)
    private String modulo;

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

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public Long getUsuarioActorId() { return usuarioActorId; }
    public void setUsuarioActorId(Long usuarioActorId) { this.usuarioActorId = usuarioActorId; }
    public String getActorCorreo() { return actorCorreo; }
    public void setActorCorreo(String actorCorreo) { this.actorCorreo = actorCorreo; }
    public String getModulo() { return modulo; }
    public void setModulo(String modulo) { this.modulo = modulo; }
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }
    public String getResumen() { return resumen; }
    public void setResumen(String resumen) { this.resumen = resumen; }
    public String getDetalleAntesJson() { return detalleAntesJson; }
    public void setDetalleAntesJson(String detalleAntesJson) { this.detalleAntesJson = detalleAntesJson; }
    public String getDetalleDespuesJson() { return detalleDespuesJson; }
    public void setDetalleDespuesJson(String detalleDespuesJson) { this.detalleDespuesJson = detalleDespuesJson; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
}
