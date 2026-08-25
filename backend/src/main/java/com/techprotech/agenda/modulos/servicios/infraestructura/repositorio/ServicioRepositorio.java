package com.techprotech.agenda.modulos.servicios.infraestructura.repositorio;

import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ServicioRepositorio extends JpaRepository<ServicioEntidad, Long> {

    @Query("""
            select s
            from ServicioEntidad s, ServicioSucursalEntidad ss
            where s.id = ss.id.servicioId
              and ss.id.sucursalId = :sucursalId
              and ss.empresaId = s.empresaId
              and ss.activo = true
              and s.activo = true
            """)
    List<ServicioEntidad> findBySucursalIdAndActivoTrue(@Param("sucursalId") Long sucursalId);

    @Query("""
            select s
            from ServicioEntidad s, ServicioSucursalEntidad ss
            where s.id = ss.id.servicioId
              and ss.id.sucursalId = :sucursalId
              and ss.empresaId = s.empresaId
              and ss.activo = true
              and s.activo = true
            order by s.ordenPublico asc, s.nombre asc
            """)
    List<ServicioEntidad> findBySucursalIdAndActivoTrueOrderByOrdenPublicoAscNombreAsc(@Param("sucursalId") Long sucursalId);

    @Query("""
            select s
            from ServicioEntidad s, ServicioSucursalEntidad ss
            where s.id = :id
              and s.id = ss.id.servicioId
              and s.empresaId = :empresaId
              and ss.empresaId = :empresaId
              and ss.id.sucursalId = :sucursalId
              and ss.activo = true
              and s.activo = true
            """)
    Optional<ServicioEntidad> findByIdAndEmpresaIdAndSucursalIdAndActivoTrue(
            @Param("id") Long id,
            @Param("empresaId") Long empresaId,
            @Param("sucursalId") Long sucursalId
    );

    List<ServicioEntidad> findByEmpresaIdOrderByNombreAsc(Long empresaId);

    List<ServicioEntidad> findByEmpresaIdOrderByOrdenPublicoAscNombreAsc(Long empresaId);

    Optional<ServicioEntidad> findByIdAndEmpresaId(Long id, Long empresaId);

    Optional<ServicioEntidad> findByIdAndEmpresaIdAndActivoTrue(Long id, Long empresaId);

    Optional<ServicioEntidad> findByEmpresaIdAndSlug(Long empresaId, String slug);

    List<ServicioEntidad> findByEmpresaIdAndIdIn(Long empresaId, Collection<Long> ids);

    List<ServicioEntidad> findByEmpresaIdAndVisiblePublicoTrueAndActivoTrueOrderByOrdenPublicoAscNombreAsc(Long empresaId);

    boolean existsByEmpresaIdAndSlug(Long empresaId, String slug);

    boolean existsByEmpresaIdAndSlugAndIdNot(Long empresaId, String slug, Long id);
}
