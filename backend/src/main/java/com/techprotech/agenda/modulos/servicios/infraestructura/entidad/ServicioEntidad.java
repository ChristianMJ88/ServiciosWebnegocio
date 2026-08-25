package com.techprotech.agenda.modulos.servicios.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "servicio")
public class ServicioEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "sucursal_id", nullable = false)
    private Long sucursalId;

    @Column(name = "grupo_id")
    private Long grupoId;

    @Column(name = "subgrupo_id")
    private Long subgrupoId;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 140)
    private String slug;

    @Column(length = 500)
    private String descripcion;

    @Column(name = "imagen_url", length = 255)
    private String imagenUrl;

    @Column(name = "duracion_minutos", nullable = false)
    private int duracionMinutos;

    @Column(name = "buffer_antes_minutos", nullable = false)
    private int bufferAntesMinutos;

    @Column(name = "buffer_despues_minutos", nullable = false)
    private int bufferDespuesMinutos;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false, length = 10)
    private String moneda;

    @Column(name = "orden_publico", nullable = false)
    private int ordenPublico;

    @Column(name = "visible_publico", nullable = false)
    private boolean visiblePublico;

    @Column(name = "requiere_anticipo", nullable = false)
    private boolean requiereAnticipo;

    @Column(name = "anticipo_tipo", length = 20)
    private String anticipoTipo;

    @Column(name = "anticipo_valor", precision = 10, scale = 2)
    private BigDecimal anticipoValor;

    @Column(nullable = false)
    private boolean activo;
}
