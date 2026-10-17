package com.techprotech.agenda.modulos.politicas.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @Entity @org.hibernate.annotations.Immutable
@Table(name="politica_empresa", uniqueConstraints=@UniqueConstraint(columnNames={"empresa_id","version"}))
public class PoliticaEmpresaEntidad {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id", updatable=false) private Long id;
    @Column(name="empresa_id", updatable=false) private Long empresaId;
    @Column(name="version", updatable=false) private Integer version;
    @Column(name="modo_no_show", updatable=false, length=20) private String modoNoShow;
    @Column(name="dias_reagendar", updatable=false) private Integer diasReagendar;
    @Column(name="dias_aclaracion", updatable=false) private Integer diasAclaracion;
    @Column(name="reagendados_max", updatable=false) private Integer reagendadosMax;
    @Column(name="recordatorio_1_dias", updatable=false) private Integer recordatorio1Dias;
    @Column(name="recordatorio_2_dias", updatable=false) private Integer recordatorio2Dias;
    @Column(name="texto_adicional", updatable=false, length=2000) private String textoAdicional;
    @Column(name="contacto_reclamos_correo", updatable=false, length=255) private String contactoReclamosCorreo;
    @Column(name="contacto_reclamos_telefono", updatable=false, length=30) private String contactoReclamosTelefono;
    @Column(name="plantilla_texto", updatable=false, length=30) private String plantillaTexto;
    @Column(name="texto_generado", updatable=false, columnDefinition="text") private String textoGenerado;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
    @Column(name="texto_sha256", updatable=false, length=64) private String textoSha256;
    @Column(name="publicada_en", updatable=false) private LocalDateTime publicadaEn;
    @Column(name="publicada_por_usuario_id", updatable=false) private Long publicadaPorUsuarioId;
}
