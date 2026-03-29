package com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.PermisoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PermisoRepositorio extends JpaRepository<PermisoEntidad, Long> {

    Optional<PermisoEntidad> findByCodigo(String codigo);

    List<PermisoEntidad> findAllByOrderByNombreAsc();

    List<PermisoEntidad> findByCodigoIn(Collection<String> codigos);
}
