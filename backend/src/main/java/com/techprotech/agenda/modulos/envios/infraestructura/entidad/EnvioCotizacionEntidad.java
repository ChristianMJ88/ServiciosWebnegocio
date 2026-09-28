package com.techprotech.agenda.modulos.envios.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "envio_cotizacion")
public class EnvioCotizacionEntidad {
    @Id @Column(length = 36) private String id;
    @Column(name = "empresa_id", nullable = false) private Long empresaId;
    @Column(name = "sucursal_id", nullable = false) private Long sucursalId;
    @Column(nullable = false, length = 40) private String proveedor;
    @Column(nullable = false, length = 20) private String ambiente;
    @Column(name = "carrito_hash", nullable = false, length = 64) private String carritoHash;
    @Column(name = "destino_hash", nullable = false, length = 64) private String destinoHash;
    @Column(nullable = false, length = 80) private String paqueteria;
    @Column(name = "paqueteria_nombre", length = 120) private String paqueteriaNombre;
    @Column(nullable = false, length = 120) private String servicio;
    @Column(name = "servicio_nombre", length = 180) private String servicioNombre;
    @Column(name = "entrega_estimada", length = 120) private String entregaEstimada;
    @Column(name = "costo_proveedor", nullable = false, precision = 12, scale = 2) private BigDecimal costoProveedor;
    @Column(name = "costo_cliente", nullable = false, precision = 12, scale = 2) private BigDecimal costoCliente;
    @Column(nullable = false, length = 10) private String moneda;
    @Column(name = "expira_en", nullable = false) private LocalDateTime expiraEn;
    @Column(name = "creado_en", insertable = false, updatable = false) private LocalDateTime creadoEn;
}
