package com.techprotech.agenda.modulos.productos.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "producto_zona_precio")
public class ZonaPrecioProductoEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;
    @Column(nullable = false, length = 120)
    private String nombre;
    @Column(nullable = false, length = 60)
    private String clave;
    @Column(length = 500)
    private String descripcion;
    @Column(nullable = false)
    private int prioridad;
    @Column(nullable = false)
    private boolean activa;
}
