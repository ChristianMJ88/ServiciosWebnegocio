package com.techprotech.agenda.modulos.sucursales.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sucursal")
public class SucursalEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 255)
    private String direccion;

    @Column(length = 30)
    private String telefono;

    @Column(name = "zona_horaria", nullable = false, length = 60)
    private String zonaHoraria;
    @Column(name="zona_precio_id") private Long zonaPrecioId;

    @Column(length=100) private String municipio;
    @Column(name="estado_region",length=100) private String estadoRegion;
    @Column(name="pais_codigo",length=2) private String paisCodigo;
    @Column(name="codigo_postal",length=12) private String codigoPostal;
    @Column(nullable = false)
    private boolean activa;

}
