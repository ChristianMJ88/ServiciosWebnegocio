package com.techprotech.agenda.modulos.servicios.infraestructura.repositorio;

import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.SubgrupoServicioEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubgrupoServicioRepositorio extends JpaRepository<SubgrupoServicioEntidad, Long> {

    List<SubgrupoServicioEntidad> findByEmpresaIdOrderByOrdenPublicoAscNombreAsc(Long empresaId);

    List<SubgrupoServicioEntidad> findByEmpresaIdAndGrupoIdOrderByOrdenPublicoAscNombreAsc(Long empresaId, Long grupoId);

    Optional<SubgrupoServicioEntidad> findByIdAndEmpresaId(Long id, Long empresaId);

    Optional<SubgrupoServicioEntidad> findByEmpresaIdAndGrupoIdAndSlug(Long empresaId, Long grupoId, String slug);

    boolean existsByEmpresaIdAndGrupoIdAndSlug(Long empresaId, Long grupoId, String slug);

    boolean existsByEmpresaIdAndGrupoIdAndSlugAndIdNot(Long empresaId, Long grupoId, String slug, Long id);
}
