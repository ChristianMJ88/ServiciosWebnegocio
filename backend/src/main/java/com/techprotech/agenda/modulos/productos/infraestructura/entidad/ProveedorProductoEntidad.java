package com.techprotech.agenda.modulos.productos.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "producto_proveedor")
public class ProveedorProductoEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;
    @Column(nullable = false, length = 160)
    private String nombre;
    @Column(length = 60)
    private String clave;
    @Column(length = 160)
    private String contacto;
    @Column(length = 180)
    private String correo;
    @Column(length = 30)
    private String telefono;
    @Column(name = "sitio_web", length = 500)
    private String sitioWeb;
    @Column(length = 1000)
    private String notas;
    @Column(nullable = false)
    private boolean activo;
}
