package com.techprotech.agenda.modulos.servicios.infraestructura.repositorio;

import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ServicioRepositorio extends JpaRepository<ServicioEntidad, Long> {

    List<ServicioEntidad> findBySucursalIdAndActivoTrue(Long sucursalId);

    Optional<ServicioEntidad> findByIdAndEmpresaIdAndSucursalIdAndActivoTrue(Long id, Long empresaId, Long sucursalId);

    List<ServicioEntidad> findByEmpresaIdOrderByNombreAsc(Long empresaId);

    Optional<ServicioEntidad> findByIdAndEmpresaId(Long id, Long empresaId);

    List<ServicioEntidad> findByEmpresaIdAndIdIn(Long empresaId, Collection<Long> ids);
}
