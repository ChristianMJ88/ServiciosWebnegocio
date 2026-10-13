package com.techprotech.agenda.modulos.pagos.infraestructura.repositorio;

import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CheckoutOrdenRepositorio extends JpaRepository<CheckoutOrdenEntidad, Long> {
    List<CheckoutOrdenEntidad> findTop100ByEmpresaIdAndRequiereConciliacionTrueOrderByIdDesc(Long empresaId);
    List<CheckoutOrdenEntidad> findTop50ByEstadoAndModalidadEntregaOrderByIdAsc(String estado, String modalidadEntrega);
    Optional<CheckoutOrdenEntidad> findByEmpresaIdAndIdempotencia(Long empresaId, String idempotencia);
    Optional<CheckoutOrdenEntidad> findByEmpresaIdAndPublicId(Long empresaId, String publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CheckoutOrdenEntidad o where o.empresaId = :empresaId and o.id = :ordenId")
    Optional<CheckoutOrdenEntidad> bloquearPorEmpresaEId(@Param("empresaId") Long empresaId,
                                                          @Param("ordenId") Long ordenId);

    List<CheckoutOrdenEntidad> findTop100ByInventarioEstadoAndExpiraEnBeforeOrderByExpiraEnAsc(
            String inventarioEstado, LocalDateTime expiraEn);
    @Query("select o from CheckoutOrdenEntidad o where o.estado in ('PAGADA', 'PAGADA_CON_INCIDENCIA') and not exists (select v.id from VentaEntidad v where v.empresaId=o.empresaId and v.checkoutOrdenId=o.id) order by o.id")
    List<CheckoutOrdenEntidad> buscarPagadasSinVenta(org.springframework.data.domain.Pageable pagina);
    List<CheckoutOrdenEntidad> findTop100ByEstadoOrderByIdAsc(String estado);
}
