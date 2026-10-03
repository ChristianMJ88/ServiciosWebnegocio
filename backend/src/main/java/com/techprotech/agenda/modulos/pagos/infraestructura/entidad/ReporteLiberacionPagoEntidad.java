package com.techprotech.agenda.modulos.pagos.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @Entity
@Table(name = "reporte_liberacion_pago")
public class ReporteLiberacionPagoEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="empresa_id", nullable=false) private Long empresaId;
    @Column(name="proveedor_pago_id", nullable=false) private Long proveedorPagoId;
    @Column(name="tarea_externa_id", nullable=false, length=100) private String tareaExternaId;
    @Column(name="reporte_externo_id", length=100) private String reporteExternoId;
    @Column(name="nombre_archivo", length=300) private String nombreArchivo;
    @Column(nullable=false, length=30) private String estado;
    @Column(name="fecha_desde", nullable=false) private LocalDateTime fechaDesde;
    @Column(name="fecha_hasta", nullable=false) private LocalDateTime fechaHasta;
    @Column(name="filas_procesadas", nullable=false) private int filasProcesadas;
    @Column(name="error_ultimo", length=1000) private String errorUltimo;
    @Column(name="solicitado_en", insertable=false, updatable=false) private LocalDateTime solicitadoEn;
    @Column(name="procesado_en") private LocalDateTime procesadoEn;
    @Column(name="actualizado_en", insertable=false, updatable=false) private LocalDateTime actualizadoEn;
}
