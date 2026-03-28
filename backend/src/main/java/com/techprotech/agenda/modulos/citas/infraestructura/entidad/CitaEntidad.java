package com.techprotech.agenda.modulos.citas.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cita")
public class CitaEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "sucursal_id", nullable = false)
    private Long sucursalId;

    @Column(name = "servicio_id", nullable = false)
    private Long servicioId;

    @Column(name = "prestador_id", nullable = false)
    private Long prestadorId;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fin;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false, length = 10)
    private String moneda;

    @Column(length = 500)
    private String notas;

    @Column(name = "creada_por_usuario_id", nullable = false)
    private Long creadaPorUsuarioId;

    @Column(name = "cancelada_en")
    private LocalDateTime canceladaEn;

    @Column(name = "motivo_cancelacion", length = 255)
    private String motivoCancelacion;

    @Column(name = "reprogramada_desde_id")
    private Long reprogramadaDesdeId;

    @Column(name = "check_in_en")
    private LocalDateTime checkInEn;

    @Column(name = "check_in_por_usuario_id")
    private Long checkInPorUsuarioId;

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

    public Long getServicioId() {
        return servicioId;
    }

    public void setServicioId(Long servicioId) {
        this.servicioId = servicioId;
    }

    public Long getPrestadorId() {
        return prestadorId;
    }

    public void setPrestadorId(Long prestadorId) {
        this.prestadorId = prestadorId;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public void setInicio(LocalDateTime inicio) {
        this.inicio = inicio;
    }

    public LocalDateTime getFin() {
        return fin;
    }

    public void setFin(LocalDateTime fin) {
        this.fin = fin;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public Long getCreadaPorUsuarioId() {
        return creadaPorUsuarioId;
    }

    public void setCreadaPorUsuarioId(Long creadaPorUsuarioId) {
        this.creadaPorUsuarioId = creadaPorUsuarioId;
    }

    public LocalDateTime getCanceladaEn() {
        return canceladaEn;
    }

    public void setCanceladaEn(LocalDateTime canceladaEn) {
        this.canceladaEn = canceladaEn;
    }

    public String getMotivoCancelacion() {
        return motivoCancelacion;
    }

    public void setMotivoCancelacion(String motivoCancelacion) {
        this.motivoCancelacion = motivoCancelacion;
    }

    public Long getReprogramadaDesdeId() {
        return reprogramadaDesdeId;
    }

    public void setReprogramadaDesdeId(Long reprogramadaDesdeId) {
        this.reprogramadaDesdeId = reprogramadaDesdeId;
    }

    public LocalDateTime getCheckInEn() {
        return checkInEn;
    }

    public void setCheckInEn(LocalDateTime checkInEn) {
        this.checkInEn = checkInEn;
    }

    public Long getCheckInPorUsuarioId() {
        return checkInPorUsuarioId;
    }

    public void setCheckInPorUsuarioId(Long checkInPorUsuarioId) {
        this.checkInPorUsuarioId = checkInPorUsuarioId;
    }
}
