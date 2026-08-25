package com.techprotech.agenda.compartido.correo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
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

    @Column(name = "proveedor_mensaje_id", length = 64)
    private String proveedorMensajeId;

    @Column(name = "estado_entrega", length = 30)
    private String estadoEntrega;

    @Column(name = "estado_entrega_actualizado_en")
    private LocalDateTime estadoEntregaActualizadoEn;

    @Column(name = "codigo_error_proveedor", length = 32)
    private String codigoErrorProveedor;

    @Column(name = "detalle_error_proveedor", length = 500)
    private String detalleErrorProveedor;


}
