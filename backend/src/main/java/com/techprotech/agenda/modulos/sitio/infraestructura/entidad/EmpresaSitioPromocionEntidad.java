package com.techprotech.agenda.modulos.sitio.infraestructura.entidad;

import com.techprotech.agenda.modulos.sitio.dominio.TipoDescuentoPromocion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "empresa_sitio_promocion")
public class EmpresaSitioPromocionEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;
    @Column(nullable = false, length = 120)
    private String titulo;
    @Column(length = 500)
    private String descripcion;
    @Column(name = "media_id")
    private Long mediaId;
    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;
    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;
    @Column(name = "fecha_fin")
    private LocalDate fechaFin;
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_descuento", nullable = false, length = 20)
    private TipoDescuentoPromocion tipoDescuento;
    @Column(name = "valor_descuento", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorDescuento;
    @Column(nullable = false, length = 10)
    private String moneda;
    @Column(name = "texto_boton", nullable = false, length = 40)
    private String textoBoton;
    @Column(nullable = false)
    private boolean activa;
    @Column(nullable = false)
    private int orden;
}
