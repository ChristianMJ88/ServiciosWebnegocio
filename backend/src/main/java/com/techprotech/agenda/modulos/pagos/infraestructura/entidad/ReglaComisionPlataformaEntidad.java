package com.techprotech.agenda.modulos.pagos.infraestructura.entidad;

import com.techprotech.agenda.modulos.pagos.dominio.TipoComisionPlataforma;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "regla_comision_plataforma")
public class ReglaComisionPlataformaEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "proveedor_pago_id", nullable = false) private Long proveedorPagoId;
    @Column(name = "empresa_id") private Long empresaId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) private TipoComisionPlataforma tipo;
    @Column(nullable = false, precision = 12, scale = 4) private BigDecimal valor;
    @Column(name = "importe_minimo", precision = 12, scale = 2) private BigDecimal importeMinimo;
    @Column(name = "importe_maximo", precision = 12, scale = 2) private BigDecimal importeMaximo;
    @Column(length = 10) private String moneda;
    @Column(nullable = false) private int prioridad;
    @Column(name = "vigente_desde", nullable = false) private LocalDateTime vigenteDesde;
    @Column(name = "vigente_hasta") private LocalDateTime vigenteHasta;
    @Column(nullable = false) private boolean activa;
}
