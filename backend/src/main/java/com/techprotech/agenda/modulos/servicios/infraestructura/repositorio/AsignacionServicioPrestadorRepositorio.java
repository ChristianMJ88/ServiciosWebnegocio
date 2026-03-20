package com.techprotech.agenda.modulos.servicios.infraestructura.repositorio;

import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.AsignacionServicioPrestadorEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.AsignacionServicioPrestadorId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AsignacionServicioPrestadorRepositorio extends JpaRepository<AsignacionServicioPrestadorEntidad, AsignacionServicioPrestadorId> {

    List<AsignacionServicioPrestadorEntidad> findByIdServicioIdAndActivaTrue(Long servicioId);

    Optional<AsignacionServicioPrestadorEntidad> findByIdPrestadorIdAndIdServicioIdAndActivaTrue(Long prestadorId, Long servicioId);

    List<AsignacionServicioPrestadorEntidad> findByIdPrestadorId(Long prestadorId);

    List<AsignacionServicioPrestadorEntidad> findByIdPrestadorIdIn(Collection<Long> prestadorIds);
}
