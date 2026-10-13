package com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio;

import com.techprotech.agenda.modulos.prestadores.infraestructura.entidad.PrestadorServicioEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PrestadorServicioRepositorio extends JpaRepository<PrestadorServicioEntidad, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PrestadorServicioEntidad p join UsuarioEntidad u on u.id=p.usuarioId where u.empresaId=:empresaId and p.usuarioId=:prestadorId")
    Optional<PrestadorServicioEntidad> bloquearPorEmpresa(Long empresaId, Long prestadorId);
    @Query("select p from PrestadorServicioEntidad p join UsuarioEntidad u on u.id=p.usuarioId " +
            "where u.empresaId=:empresaId and u.habilitado=true and p.sucursalId=:sucursalId and p.activo=true order by p.nombreMostrar")
    List<PrestadorServicioEntidad> findPublicosPorEmpresaYSucursal(@Param("empresaId") Long empresaId,@Param("sucursalId") Long sucursalId);

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
