package com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.RolEmpresaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RolEmpresaRepositorio extends JpaRepository<RolEmpresaEntidad, Long> {

    List<RolEmpresaEntidad> findByEmpresaIdOrderByNombreAsc(Long empresaId);

    Optional<RolEmpresaEntidad> findByEmpresaIdAndCodigo(Long empresaId, String codigo);

    List<RolEmpresaEntidad> findByEmpresaIdAndIdIn(Long empresaId, Collection<Long> ids);

    boolean existsByEmpresaIdAndCodigo(Long empresaId, String codigo);

    long countByEmpresaIdAndActivoTrue(Long empresaId);
}
