package com.techprotech.agenda.modulos.sitio.infraestructura.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "empresa_sitio_promocion_servicio")
public class EmpresaSitioPromocionServicioEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;
    @Column(name = "promocion_id", nullable = false)
    private Long promocionId;
    @Column(name = "servicio_id", nullable = false)
    private Long servicioId;
}
