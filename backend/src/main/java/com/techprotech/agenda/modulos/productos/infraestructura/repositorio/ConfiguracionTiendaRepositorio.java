package com.techprotech.agenda.modulos.productos.infraestructura.repositorio;

import com.techprotech.agenda.modulos.productos.infraestructura.entidad.ConfiguracionTiendaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionTiendaRepositorio extends JpaRepository<ConfiguracionTiendaEntidad, Long> {
}
