package com.techprotech.agenda.modulos.envios.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sucursal_envio_origen")
public class SucursalEnvioOrigenEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "empresa_id", nullable = false) private Long empresaId;
    @Column(name = "sucursal_id", nullable = false) private Long sucursalId;
    @Column(nullable = false) private boolean activo = true;
    @Column(name = "nombre_contacto", nullable = false, length = 180) private String nombreContacto;
    @Column(nullable = false, length = 30) private String telefono;
    @Column(length = 180) private String correo;
    @Column(nullable = false, length = 180) private String calle;
    @Column(name = "numero_exterior", nullable = false, length = 30) private String numeroExterior;
    @Column(name = "numero_interior", length = 30) private String numeroInterior;
    @Column(nullable = false, length = 150) private String colonia;
    @Column(nullable = false, length = 150) private String municipio;
    @Column(nullable = false, length = 120) private String estado;
    @Column(name = "codigo_postal", nullable = false, length = 12) private String codigoPostal;
    @Column(name = "codigo_pais", nullable = false, length = 2) private String codigoPais = "MX";
    @Column(length = 500) private String referencias;
}
