package com.techprotech.agenda.modulos.caja.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "caja_sesion")
public class CajaSesionEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "sucursal_id", nullable = false)
    private Long sucursalId;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "monto_inicial", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoInicial;

    @Column(name = "monto_esperado", precision = 10, scale = 2)
    private BigDecimal montoEsperado;

    @Column(name = "monto_contado", precision = 10, scale = 2)
    private BigDecimal montoContado;

    @Column(precision = 10, scale = 2)
    private BigDecimal diferencia;

    @Column(length = 500)
    private String observaciones;

    @Column(name = "abierta_por_usuario_id", nullable = false)
    private Long abiertaPorUsuarioId;

    @Column(name = "abierta_en", nullable = false)
    private LocalDateTime abiertaEn;

    @Column(name = "cerrada_por_usuario_id")
    private Long cerradaPorUsuarioId;

    @Column(name = "cerrada_en")
    private LocalDateTime cerradaEn;

}
