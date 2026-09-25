package com.techprotech.agenda.modulos.productos.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "producto_categoria")
public class CategoriaProductoEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;
    @Column(name = "categoria_padre_id")
    private Long categoriaPadreId;
    @Column(nullable = false, length = 120)
    private String nombre;
    @Column(nullable = false, length = 140)
    private String slug;
    @Column(length = 500)
    private String descripcion;
    @Column(nullable = false)
    private int orden;
    @Column(nullable = false)
    private boolean activa;
}
