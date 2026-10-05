package com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio;

import com.techprotech.agenda.modulos.prestadores.infraestructura.entidad.PrestadorServicioEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PrestadorServicioRepositorio extends JpaRepository<PrestadorServicioEntidad, Long> {

    Optional<PrestadorServicioEntidad> findByUsuarioIdAndSucursalIdAndActivoTrue(Long usuarioId, Long sucursalId);

    List<PrestadorServicioEntidad> findBySucursalIdInOrderByNombreMostrarAsc(Collection<Long> sucursalIds);

    @Query("""
            select count(prestador)
            from PrestadorServicioEntidad prestador
            join UsuarioEntidad usuario on usuario.id = prestador.usuarioId
            where usuario.empresaId = :empresaId
              and usuario.habilitado = true
              and prestador.activo = true
            """)
    long countActivosByEmpresaId(@Param("empresaId") Long empresaId);
}
