package com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio;

import com.techprotech.agenda.modulos.prestadores.infraestructura.entidad.PrestadorServicioEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PrestadorServicioRepositorio extends JpaRepository<PrestadorServicioEntidad, Long> {

    Optional<PrestadorServicioEntidad> findByUsuarioIdAndSucursalIdAndActivoTrue(Long usuarioId, Long sucursalId);

    List<PrestadorServicioEntidad> findBySucursalIdInOrderByNombreMostrarAsc(Collection<Long> sucursalIds);
}
