package com.techprotech.agenda.modulos.caja.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    public Long getId() {
        return id;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public Long getSucursalId() {
        return sucursalId;
    }

    public void setSucursalId(Long sucursalId) {
        this.sucursalId = sucursalId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public BigDecimal getMontoInicial() {
        return montoInicial;
    }

    public void setMontoInicial(BigDecimal montoInicial) {
        this.montoInicial = montoInicial;
    }

    public BigDecimal getMontoEsperado() {
        return montoEsperado;
    }

    public void setMontoEsperado(BigDecimal montoEsperado) {
        this.montoEsperado = montoEsperado;
    }

    public BigDecimal getMontoContado() {
        return montoContado;
    }

    public void setMontoContado(BigDecimal montoContado) {
        this.montoContado = montoContado;
    }

    public BigDecimal getDiferencia() {
        return diferencia;
    }

    public void setDiferencia(BigDecimal diferencia) {
        this.diferencia = diferencia;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public Long getAbiertaPorUsuarioId() {
        return abiertaPorUsuarioId;
    }

    public void setAbiertaPorUsuarioId(Long abiertaPorUsuarioId) {
        this.abiertaPorUsuarioId = abiertaPorUsuarioId;
    }

    public LocalDateTime getAbiertaEn() {
        return abiertaEn;
    }

    public void setAbiertaEn(LocalDateTime abiertaEn) {
        this.abiertaEn = abiertaEn;
    }

    public Long getCerradaPorUsuarioId() {
        return cerradaPorUsuarioId;
    }

    public void setCerradaPorUsuarioId(Long cerradaPorUsuarioId) {
        this.cerradaPorUsuarioId = cerradaPorUsuarioId;
    }

    public LocalDateTime getCerradaEn() {
        return cerradaEn;
    }

    public void setCerradaEn(LocalDateTime cerradaEn) {
        this.cerradaEn = cerradaEn;
    }
}
