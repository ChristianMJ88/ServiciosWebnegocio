package com.techprotech.agenda.modulos.servicios.infraestructura.repositorio;

import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.GrupoServicioEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GrupoServicioRepositorio extends JpaRepository<GrupoServicioEntidad, Long> {

    List<GrupoServicioEntidad> findByEmpresaIdOrderByOrdenPublicoAscNombreAsc(Long empresaId);

    Optional<GrupoServicioEntidad> findByIdAndEmpresaId(Long id, Long empresaId);

    Optional<GrupoServicioEntidad> findByEmpresaIdAndSlug(Long empresaId, String slug);

    boolean existsByEmpresaIdAndSlug(Long empresaId, String slug);

    boolean existsByEmpresaIdAndSlugAndIdNot(Long empresaId, String slug, Long id);
}
