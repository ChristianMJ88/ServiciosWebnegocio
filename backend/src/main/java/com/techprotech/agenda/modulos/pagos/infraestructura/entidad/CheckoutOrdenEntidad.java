package com.techprotech.agenda.modulos.pagos.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "checkout_orden")
public class CheckoutOrdenEntidad {
  @Column(name = "requiere_conciliacion", nullable = false)
  private boolean requiereConciliacion;
  @Column(name = "motivo_incidencia", length = 500)
  private String motivoIncidencia;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "public_id", nullable = false, unique = true, length = 36)
  private String publicId;
  @Column(name = "empresa_id", nullable = false)
  private Long empresaId;
  @Column(name = "sucursal_id", nullable = false)
  private Long sucursalId;
  @Column(name = "zona_precio_id")
  private Long zonaPrecioId;
  @Column(nullable = false, length = 128)
  private String idempotencia;
  @Column(nullable = false, length = 30)
  private String estado;
  @Column(name = "inventario_estado", nullable = false, length = 20)
  private String inventarioEstado;
  @Column(name = "correo_cliente", nullable = false, length = 180)
  private String correoCliente;
  @Column(name = "modalidad_entrega", nullable = false, length = 30)
  private String modalidadEntrega;
  @Column(name = "nombre_destinatario", length = 180)
  private String nombreDestinatario;
  @Column(name = "telefono_destinatario", length = 30)
  private String telefonoDestinatario;
  @Column(length = 180)
  private String calle;
  @Column(name = "numero_exterior", length = 30)
  private String numeroExterior;
  @Column(name = "numero_interior", length = 30)
  private String numeroInterior;
  @Column(length = 150)
  private String colonia;
  @Column(length = 150)
  private String municipio;
  @Column(name = "estado_direccion", length = 120)
  private String estadoDireccion;
  @Column(name = "codigo_postal", length = 12)
  private String codigoPostal;
  @Column(name = "codigo_pais", length = 2)
  private String codigoPais;
  @Column(name = "referencias_direccion", length = 500)
  private String referenciasDireccion;
  @Column(name = "direccion_formateada", length = 500)
  private String direccionFormateada;
  @Column(name = "direccion_validacion_estado", nullable = false, length = 30)
  private String direccionValidacionEstado = "NO_APLICA";
  @Column(name = "google_place_id", length = 255)
  private String googlePlaceId;
  @Column(precision = 10, scale = 7)
  private BigDecimal latitud;
  @Column(precision = 10, scale = 7)
  private BigDecimal longitud;
  @Column(name = "direccion_validada_en")
  private LocalDateTime direccionValidadaEn;
  @Column(nullable = false, length = 10)
  private String moneda;
  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal subtotal;
  @Column(name = "descuento_total", nullable = false, precision = 12, scale = 2)
  private BigDecimal descuentoTotal;
  @Column(name = "impuestos_total", nullable = false, precision = 12, scale = 2)
  private BigDecimal impuestosTotal;
  @Column(name = "costo_envio", nullable = false, precision = 12, scale = 2)
  private BigDecimal costoEnvio;
  @Column(name = "envio_cotizacion_id", length = 36)
  private String envioCotizacionId;
  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal total;
  @Column(name = "expira_en", nullable = false)
  private LocalDateTime expiraEn;
  @Column(name = "creada_en", insertable = false, updatable = false)
  private LocalDateTime creadaEn;
  @Column(name = "actualizada_en", insertable = false, updatable = false)
  private LocalDateTime actualizadaEn;
}
