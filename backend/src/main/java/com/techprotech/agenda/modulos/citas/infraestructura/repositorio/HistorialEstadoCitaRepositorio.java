package com.techprotech.agenda.modulos.citas.infraestructura.repositorio;

import com.techprotech.agenda.modulos.citas.infraestructura.entidad.HistorialEstadoCitaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialEstadoCitaRepositorio extends JpaRepository<HistorialEstadoCitaEntidad, Long> {
}

