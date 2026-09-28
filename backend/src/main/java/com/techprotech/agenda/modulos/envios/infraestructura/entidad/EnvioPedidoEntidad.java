package com.techprotech.agenda.modulos.envios.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "envio_pedido")
public class EnvioPedidoEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "empresa_id", nullable = false) private Long empresaId;
    @Column(name = "checkout_orden_id", nullable = false) private Long checkoutOrdenId;
    @Column(name = "envio_cotizacion_id", length = 36) private String envioCotizacionId;
    @Column(name = "sucursal_id", nullable = false) private Long sucursalId;
    @Column(nullable = false, length = 40) private String proveedor;
    @Column(nullable = false, length = 20) private String ambiente;
    @Column(nullable = false, length = 40) private String estado;
    @Column(name = "motivo_bloqueo", length = 500) private String motivoBloqueo;
    @Column(length = 80) private String paqueteria;
    @Column(length = 120) private String servicio;
    @Column(name = "numero_guia", length = 160) private String numeroGuia;
    @Column(name = "url_etiqueta", length = 1000) private String urlEtiqueta;
    @Column(name = "estado_rastreo", length = 80) private String estadoRastreo;
    @Column(name = "costo_proveedor", precision = 12, scale = 2) private BigDecimal costoProveedor;
    @Column(name = "costo_cliente", nullable = false, precision = 12, scale = 2) private BigDecimal costoCliente;
    @Column(nullable = false, length = 10) private String moneda;
    @Column(nullable = false) private int intentos;
    @Column(name = "ultimo_error", length = 1000) private String ultimoError;
    @Column(name = "creado_en", insertable = false, updatable = false) private LocalDateTime creadoEn;
    @Column(name = "actualizado_en", insertable = false, updatable = false) private LocalDateTime actualizadoEn;
}
