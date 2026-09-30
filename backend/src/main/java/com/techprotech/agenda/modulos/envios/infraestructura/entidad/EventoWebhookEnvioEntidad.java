package com.techprotech.agenda.modulos.envios.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "evento_webhook_envio")
public class EventoWebhookEnvioEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "empresa_id", nullable = false) private Long empresaId;
    @Column(nullable = false, length = 40) private String proveedor;
    @Column(name = "evento_externo_id", nullable = false, length = 190) private String eventoExternoId;
    @Column(nullable = false, length = 100) private String tipo;
    @Column(name = "estado_procesamiento", nullable = false, length = 30) private String estadoProcesamiento;
    @Column(nullable = false) private int intentos;
    @Column(nullable = false, columnDefinition = "MEDIUMTEXT") private String payload;
    @Column(name = "error_ultimo", length = 1000) private String errorUltimo;
    @Column(name = "recibido_en", insertable = false, updatable = false) private LocalDateTime recibidoEn;
    @Column(name = "procesado_en") private LocalDateTime procesadoEn;
}
