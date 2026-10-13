package com.techprotech.agenda.modulos.pagos.infraestructura.repositorio;

import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.IntentoPagoCheckoutEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IntentoPagoCheckoutRepositorio extends JpaRepository<IntentoPagoCheckoutEntidad, Long> {
    @org.springframework.data.jpa.repository.Query("select i.id from IntentoPagoCheckoutEntidad i join CheckoutOrdenEntidad o on o.id=i.checkoutOrdenId and o.empresaId=i.empresaId where o.estado='PAGO_EN_PROCESO' and i.estado in ('PROCESANDO','REQUIERE_ACCION') and (i.pagoExpiraEn<=:ahora or i.pagoExpiraEn is null) order by i.id")
    java.util.List<Long> buscarIdsParaRecuperacion(java.time.LocalDateTime ahora, org.springframework.data.domain.Pageable pagina);
    Optional<IntentoPagoCheckoutEntidad> findByEmpresaIdAndIdempotencia(Long empresaId, String idempotencia);
    Optional<IntentoPagoCheckoutEntidad> findByProveedorPagoIdAndReferenciaExterna(Long proveedorPagoId,
                                                                                   String referenciaExterna);
    Optional<IntentoPagoCheckoutEntidad> findFirstByEmpresaIdAndCheckoutOrdenIdAndEstadoOrderByIdDesc(
            Long empresaId, Long checkoutOrdenId, String estado);
    Optional<IntentoPagoCheckoutEntidad> findFirstByEmpresaIdAndCheckoutOrdenIdOrderByIdDesc(
            Long empresaId, Long checkoutOrdenId);
}
