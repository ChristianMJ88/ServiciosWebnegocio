package com.techprotech.agenda.modulos.sitio.infraestructura.entidad;

import com.techprotech.agenda.modulos.sitio.dominio.VistaServiciosDestacados;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "empresa_sitio_servicios_destacados_config")
public class EmpresaSitioServiciosDestacadosConfigEntidad {
    @Id
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VistaServiciosDestacados vista;

    @Column(name = "mostrar_precio", nullable = false)
    private boolean mostrarPrecio;

    @Column(name = "mostrar_duracion", nullable = false)
    private boolean mostrarDuracion;

    @Column(name = "mostrar_descripcion", nullable = false)
    private boolean mostrarDescripcion;

    @Column(nullable = false)
    private int limite;
}
