package com.techprotech.agenda.modulos.plataforma.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @Entity
@Table(name="plataforma_suscripcion")
public class SuscripcionPlataformaEntidad {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="empresa_id", nullable=false) private Long empresaId;
    @Column(name="plan_id", nullable=false) private Long planId;
    @Column(nullable=false, length=30) private String estado;
    @Column(nullable=false, length=20) private String ciclo;
    @Column(name="precio_acordado", nullable=false, precision=12, scale=2) private BigDecimal precioAcordado;
    @Column(nullable=false, length=10) private String moneda;
    @Column(name="inicia_en", nullable=false) private LocalDateTime iniciaEn;
    @Column(name="termina_en") private LocalDateTime terminaEn;
    @Column(name="prueba_hasta") private LocalDateTime pruebaHasta;
    @Column(name="cancelada_en") private LocalDateTime canceladaEn;
    @Column(name="proveedor_pago_id") private Long proveedorPagoId;
    @Column(name="referencia_externa", length=180) private String referenciaExterna;
    @Column(name="creada_en", insertable=false, updatable=false) private LocalDateTime creadaEn;
    @Column(name="actualizada_en", insertable=false, updatable=false) private LocalDateTime actualizadaEn;
}
