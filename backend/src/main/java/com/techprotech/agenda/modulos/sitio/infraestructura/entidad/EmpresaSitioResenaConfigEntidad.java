package com.techprotech.agenda.modulos.sitio.infraestructura.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "empresa_sitio_resena_config")
public class EmpresaSitioResenaConfigEntidad {
    @Id
    @Column(name = "empresa_id")
    private Long empresaId;

    @Column(name = "google_maps_url", length = 500)
    private String googleMapsUrl;
}
