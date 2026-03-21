package com.techprotech.agenda.compartido.correo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "bandeja_salida_notificacion")
public class BandejaSalidaNotificacionEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "tipo_agregado", nullable = false, length = 50)
    private String tipoAgregado;

    @Column(name = "agregado_id", nullable = false)
    private Long agregadoId;

    @Column(name = "tipo_evento", nullable = false, length = 50)
    private String tipoEvento;

    @Column(nullable = false, length = 30)
    private String canal;

    @Column(name = "payload_json", nullable = false, columnDefinition = "json")
    private String payloadJson;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "programada_en", nullable = false)
    private LocalDateTime programadaEn;

    @Column(name = "enviada_en")
    private LocalDateTime enviadaEn;

    @Column(name = "mensaje_error", length = 500)
    private String mensajeError;

    @Column(nullable = false)
    private int intentos;

    public Long getId() {
        return id;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public String getTipoAgregado() {
        return tipoAgregado;
    }

    public void setTipoAgregado(String tipoAgregado) {
        this.tipoAgregado = tipoAgregado;
    }

    public Long getAgregadoId() {
        return agregadoId;
    }

    public void setAgregadoId(Long agregadoId) {
        this.agregadoId = agregadoId;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public String getCanal() {
        return canal;
    }

    public void setCanal(String canal) {
        this.canal = canal;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getProgramadaEn() {
        return programadaEn;
    }

    public void setProgramadaEn(LocalDateTime programadaEn) {
        this.programadaEn = programadaEn;
    }

    public LocalDateTime getEnviadaEn() {
        return enviadaEn;
    }

    public void setEnviadaEn(LocalDateTime enviadaEn) {
        this.enviadaEn = enviadaEn;
    }

    public String getMensajeError() {
        return mensajeError;
    }

    public void setMensajeError(String mensajeError) {
        this.mensajeError = mensajeError;
    }

    public int getIntentos() {
        return intentos;
    }

    public void setIntentos(int intentos) {
        this.intentos = intentos;
    }
}
