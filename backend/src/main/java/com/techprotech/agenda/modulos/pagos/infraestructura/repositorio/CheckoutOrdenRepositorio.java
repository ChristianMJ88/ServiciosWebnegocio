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
    List<CheckoutOrdenEntidad> findTop50ByEstadoAndModalidadEntregaOrderByIdAsc(String estado, String modalidadEntrega);
    Optional<CheckoutOrdenEntidad> findByEmpresaIdAndIdempotencia(Long empresaId, String idempotencia);
    Optional<CheckoutOrdenEntidad> findByEmpresaIdAndPublicId(Long empresaId, String publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CheckoutOrdenEntidad o where o.empresaId = :empresaId and o.id = :ordenId")
    Optional<CheckoutOrdenEntidad> bloquearPorEmpresaEId(@Param("empresaId") Long empresaId,
                                                          @Param("ordenId") Long ordenId);

    List<CheckoutOrdenEntidad> findTop100ByInventarioEstadoAndExpiraEnBeforeOrderByExpiraEnAsc(
            String inventarioEstado, LocalDateTime expiraEn);
    List<CheckoutOrdenEntidad> findTop100ByEstadoOrderByIdAsc(String estado);
}
