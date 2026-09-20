package com.techprotech.agenda.modulos.sitio.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "empresa_sitio_promocion_sucursal")
public class EmpresaSitioPromocionSucursalEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;
    @Column(name = "promocion_id", nullable = false)
    private Long promocionId;
    @Column(name = "sucursal_id", nullable = false)
    private Long sucursalId;
}
