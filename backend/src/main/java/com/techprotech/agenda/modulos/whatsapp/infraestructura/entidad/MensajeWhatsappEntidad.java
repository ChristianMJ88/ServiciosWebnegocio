package com.techprotech.agenda.modulos.whatsapp.infraestructura.entidad;

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
@Table(name = "whatsapp_mensaje")
public class MensajeWhatsappEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "telefono_normalizado", nullable = false, length = 30)
    private String telefonoNormalizado;

    @Column(nullable = false, length = 12)
    private String direccion;

    @Column(columnDefinition = "text")
    private String cuerpo;

    @Column(name = "content_sid", length = 80)
    private String contentSid;

    @Column(name = "proveedor_mensaje_id", length = 80)
    private String proveedorMensajeId;

    @Column(length = 30)
    private String estado;

    @Column(name = "codigo_error_proveedor", length = 32)
    private String codigoErrorProveedor;

    @Column(name = "detalle_error_proveedor", length = 500)
    private String detalleErrorProveedor;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;
}
