package com.techprotech.agenda.modulos.productos.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "producto_tienda_config")
public class ConfiguracionTiendaEntidad {
    @Id
    @Column(name = "empresa_id")
    private Long empresaId;
    @Column(nullable = false)
    private boolean habilitada;
    @Column(name = "menu_mostrar_productos", nullable = false)
    private boolean menuMostrarProductos = true;
    @Column(length = 160)
    private String titulo;
    @Column(length = 500)
    private String descripcion;
    @Column(name = "seo_titulo", length = 70)
    private String seoTitulo;
    @Column(name = "seo_descripcion", length = 170)
    private String seoDescripcion;
    @Column(name = "texto_agregar", length = 40)
    private String textoAgregar;
    @Column(name = "carrito_habilitado", nullable = false)
    private boolean carritoHabilitado = true;
    @Column(name = "mostrar_stock", nullable = false)
    private boolean mostrarStock;
    @Column(name = "permitir_pedido_whatsapp", nullable = false)
    private boolean permitirPedidoWhatsapp = true;
    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;
    @Column(name="dias_nuevo",nullable=false) private int diasNuevo;
}
