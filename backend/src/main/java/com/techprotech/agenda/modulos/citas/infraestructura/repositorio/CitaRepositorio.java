package com.techprotech.agenda.modulos.citas.infraestructura.repositorio;

import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CitaRepositorio extends JpaRepository<CitaEntidad, Long> {
    List<CitaEntidad> findByEmpresaIdAndSucursalIdAndInicioBetweenAndEstadoInOrderByInicioAsc(
            Long empresaId, Long sucursalId, LocalDateTime desde, LocalDateTime hasta, List<String> estados);
    @Query("select distinct c.prestadorId from CitaEntidad c where c.empresaId=:empresaId and (c.id=:id or (:grupo is not null and c.reservaGrupoId=:grupo)) order by c.prestadorId")
    List<Long> buscarPrestadoresReserva(Long empresaId, Long id, String grupo);
    @Query("select c.id from CitaEntidad c where c.empresaId=:empresaId and (c.id=:id or (:grupo is not null and c.reservaGrupoId=:grupo)) order by c.id")
    List<Long> buscarMiembrosReserva(Long empresaId, Long id, String grupo);
    interface ReferenciaReserva {
        Long getEmpresaId();
        String getReservaGrupoId();
    }
    @Query("select c.empresaId as empresaId, c.reservaGrupoId as reservaGrupoId from CitaEntidad c where c.id=:id")
    Optional<ReferenciaReserva> buscarReferenciaReserva(Long id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CitaEntidad c where c.prestadorId=:prestadorId and c.estado in :estados and c.inicio < :fin and c.fin > :inicio")
    List<CitaEntidad> buscarConflictosBloqueando(Long prestadorId, LocalDateTime inicio, LocalDateTime fin, List<String> estados);
    @Query("select c.id from CitaEntidad c where c.estado = 'PENDIENTE_PAGO' and c.pagoExpiraEn <= :limite order by c.pagoExpiraEn")
    List<Long> buscarIdsApartadosVencidos(LocalDateTime limite, org.springframework.data.domain.Pageable pagina);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CitaEntidad c where c.id = :id and c.empresaId = :empresaId")
    Optional<CitaEntidad> bloquearPorIdYEmpresa(Long id, Long empresaId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CitaEntidad c where c.id = :id")
    Optional<CitaEntidad> bloquearPorId(Long id);

    @Query("select c from CitaEntidad c where c.empresaId=:empresaId and c.prestadorId in :prestadores and c.estado in :estados and c.inicio < :fin and c.fin > :inicio")
    List<CitaEntidad> buscarOcupadasRango(Long empresaId, Collection<Long> prestadores, LocalDateTime inicio, LocalDateTime fin, List<String> estados);
@org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
List<CitaEntidad> findByEmpresaIdAndReservaGrupoIdOrderByIdAsc(Long empresaId,String reservaGrupoId);
List<CitaEntidad> findTop100ByEstadoAndPagoExpiraEnBeforeOrderByPagoExpiraEnAsc(String estado,LocalDateTime limite);

    interface ConteoEstadoPrestador {
        String getEstado();
        long getCantidad();
    }

    @Query("select c.estado as estado, count(c) as cantidad from CitaEntidad c where c.empresaId = :empresaId and c.prestadorId = :prestadorId and c.fin > :ahora group by c.estado order by c.estado")
    List<ConteoEstadoPrestador> contarCitasFuturasPorEstado(Long empresaId, Long prestadorId, LocalDateTime ahora);

    @Query("""
            select c from CitaEntidad c
            where c.prestadorId = :prestadorId
              and c.estado in :estados
              and c.inicio < :fin
              and c.fin > :inicio
            """)
    List<CitaEntidad> buscarConflictos(Long prestadorId, LocalDateTime inicio, LocalDateTime fin, List<String> estados);

    List<CitaEntidad> findByEmpresaIdAndClienteIdOrderByInicioDesc(Long empresaId, Long clienteId);

    Optional<CitaEntidad> findByIdAndEmpresaIdAndClienteId(Long id, Long empresaId, Long clienteId);

    List<CitaEntidad> findByEmpresaIdAndPrestadorIdAndInicioBetweenOrderByInicioAsc(
            Long empresaId,
            Long prestadorId,
            LocalDateTime inicio,
            LocalDateTime fin
    );

    Optional<CitaEntidad> findByIdAndEmpresaIdAndPrestadorId(Long id, Long empresaId, Long prestadorId);

    Optional<CitaEntidad> findByIdAndEmpresaId(Long id, Long empresaId);

    List<CitaEntidad> findByEmpresaIdOrderByInicioDesc(Long empresaId);

    List<CitaEntidad> findByEmpresaIdAndEstadoInOrderByInicioDesc(Long empresaId, Collection<String> estados);

    List<CitaEntidad> findByEmpresaIdAndInicioBetweenOrderByInicioAsc(
            Long empresaId,
            LocalDateTime inicio,
            LocalDateTime fin
    );

    List<CitaEntidad> findByEmpresaIdAndInicioBetweenAndEstadoInOrderByInicioAsc(
            Long empresaId,
            LocalDateTime inicio,
            LocalDateTime fin,
            List<String> estados
    );

    List<CitaEntidad> findByEmpresaIdAndInicioBeforeAndEstadoOrderByInicioAsc(
            Long empresaId,
            LocalDateTime inicio,
            String estado
    );

    long countByEmpresaId(Long empresaId);

    long countByEmpresaIdAndEstado(Long empresaId, String estado);

    long countByEmpresaIdAndClienteId(Long empresaId, Long clienteId);

    Optional<CitaEntidad> findFirstByEmpresaIdAndClienteIdOrderByInicioDesc(Long empresaId, Long clienteId);
}
