package com.techprotech.agenda.modulos.admin.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "invitacion_usuario_empresa")
public class InvitacionUsuarioEmpresaEntidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;
    @Column(nullable = false, length = 190)
    private String correo;
    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;
    @Column(length = 30)
    private String telefono;
    @Column(length = 80)
    private String puesto;
    @Column(name = "rol_empresa_id")
    private Long rolEmpresaId;
    @Column(name = "sucursal_id")
    private Long sucursalId;
    @Column(name = "sucursal_ids_json", columnDefinition = "json")
    private String sucursalIdsJson;
    @Column(name = "permisos_directos_json", columnDefinition = "json")
    private String permisosDirectosJson;
    @Column(length = 500)
    private String notas;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(nullable = false, length = 20)
    private String estado;
    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;
    @Column(name = "aceptada_en")
    private LocalDateTime aceptadaEn;
    @Column(name = "cancelada_en")
    private LocalDateTime canceladaEn;
    @Column(name = "creada_por_usuario_id", nullable = false)
    private Long creadaPorUsuarioId;
    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;
    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;
}
