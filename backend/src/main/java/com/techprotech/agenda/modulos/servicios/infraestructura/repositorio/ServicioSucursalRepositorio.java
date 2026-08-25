package com.techprotech.agenda.modulos.servicios.infraestructura.repositorio;

import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioSucursalEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioSucursalId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ServicioSucursalRepositorio extends JpaRepository<ServicioSucursalEntidad, ServicioSucursalId> {

    Optional<ServicioSucursalEntidad> findByEmpresaIdAndIdServicioIdAndIdSucursalId(Long empresaId, Long servicioId, Long sucursalId);

    boolean existsByEmpresaIdAndIdServicioIdAndIdSucursalIdAndActivoTrue(Long empresaId, Long servicioId, Long sucursalId);

    List<ServicioSucursalEntidad> findByEmpresaIdAndIdServicioId(Long empresaId, Long servicioId);

    List<ServicioSucursalEntidad> findByEmpresaIdAndIdServicioIdAndActivoTrue(Long empresaId, Long servicioId);

    List<ServicioSucursalEntidad> findByEmpresaIdAndIdServicioIdInAndActivoTrue(Long empresaId, Collection<Long> servicioIds);

    List<ServicioSucursalEntidad> findByEmpresaIdAndIdSucursalIdAndActivoTrue(Long empresaId, Long sucursalId);

    List<ServicioSucursalEntidad> findByEmpresaIdAndIdSucursalIdInAndActivoTrue(Long empresaId, Collection<Long> sucursalIds);
}
