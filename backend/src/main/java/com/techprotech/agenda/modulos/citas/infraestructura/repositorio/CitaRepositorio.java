package com.techprotech.agenda.modulos.citas.infraestructura.repositorio;

import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CitaRepositorio extends JpaRepository<CitaEntidad, Long> {
    @Query("select count(c) > 0 from CitaEntidad c where c.empresaId=:empresaId and (c.folio=:base or c.folio like concat(:base,'-%'))")
    boolean existeFolioReserva(Long empresaId, String base);
    @org.springframework.data.jpa.repository.Modifying
    @Query(value="UPDATE cita SET politica_version_id=:politicaId, politica_aceptada_en=:aceptadaEn " +
            "WHERE id=:citaId AND empresa_id=:empresaId AND politica_version_id IS NULL", nativeQuery=true)
    int acreditarPoliticaSiAusente(Long empresaId, Long citaId, Long politicaId, LocalDateTime aceptadaEn);

    // Lecturas en READ_COMMITTED después de bloquear prestadores y citas: sin bloquear huecos de pagos.
    @Query(value="select p.cita_id from pago_cita p where p.empresa_id=:empresaId and p.cita_id in (:ids) and p.monto>0", nativeQuery=true)
    List<Long> buscarCitasConAnticipoDirecto(Long empresaId, Collection<Long> ids);

    @Query(value="select d.cita_id from venta_detalle d join venta v on v.id=d.venta_id and v.empresa_id=d.empresa_id " +
            "where d.empresa_id=:empresaId and d.cita_id in (:ids) and d.tipo_concepto='SERVICIO' and d.total>0 and v.estado='PAGADA' " +
            "and (v.checkout_orden_id is null or v.checkout_orden_id<>:ordenId)", nativeQuery=true)
    List<Long> buscarCitasConVentaAnterior(Long empresaId, Collection<Long> ids, Long ordenId);

    default List<Long> buscarCitasConPagoAnterior(Long empresaId, Collection<Long> ids, Long ordenId) {
        var resultado = new java.util.HashSet<>(buscarCitasConAnticipoDirecto(empresaId, ids));
        resultado.addAll(buscarCitasConVentaAnterior(empresaId, ids, ordenId));
        return List.copyOf(resultado);
    }
    // Mismo predicado para lista y conteo. El desglose y estado siguen en ServicioSaldoCitas.
    // D2 deberá descontar aquí los ajustes condonados cuando se añada su persistencia.
    String COBRABLE_CON_SALDO = "c.empresaId=:empresaId and c.sucursalId=:sucursalId " +
            "and c.estado in :estados " +
            "and (c.estado='FINALIZADA' or c.checkInEn is not null) " +
            "and c.precio > coalesce((select sum(p.monto) from PagoCitaEntidad p " +
                "where p.empresaId=c.empresaId and p.citaId=c.id),0) " +
            "+ coalesce((select sum(d.total) from VentaDetalleEntidad d, VentaEntidad v " +
                "where d.ventaId=v.id and d.empresaId=c.empresaId and v.empresaId=c.empresaId " +
                "and d.citaId=c.id and d.tipoConcepto='SERVICIO' and v.estado='PAGADA'),0)";

    String COBRABLE_CON_SALDO_EN_RANGO = COBRABLE_CON_SALDO + " and c.inicio >= :desde and c.inicio < :hasta";

    @Query("select c from CitaEntidad c where " + COBRABLE_CON_SALDO_EN_RANGO + " order by c.inicio desc, c.id desc")
    List<CitaEntidad> buscarCobrablesEnRango(Long empresaId, Long sucursalId, LocalDateTime desde,
            LocalDateTime hasta, Collection<String> estados);

    @Query("select count(c) from CitaEntidad c where " + COBRABLE_CON_SALDO_EN_RANGO)
    long contarCobrablesEnRango(Long empresaId, Long sucursalId, LocalDateTime desde,
            LocalDateTime hasta, Collection<String> estados);

    List<CitaEntidad> findByEmpresaIdAndIdIn(Long empresaId, Collection<Long> ids);
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
