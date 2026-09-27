package com.techprotech.agenda.modulos.pagos.infraestructura.repositorio;

import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.IntentoPagoCheckoutEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IntentoPagoCheckoutRepositorio extends JpaRepository<IntentoPagoCheckoutEntidad, Long> {
    Optional<IntentoPagoCheckoutEntidad> findByEmpresaIdAndIdempotencia(Long empresaId, String idempotencia);
    Optional<IntentoPagoCheckoutEntidad> findByProveedorPagoIdAndReferenciaExterna(Long proveedorPagoId,
                                                                                   String referenciaExterna);
    Optional<IntentoPagoCheckoutEntidad> findFirstByEmpresaIdAndCheckoutOrdenIdAndEstadoOrderByIdDesc(
            Long empresaId, Long checkoutOrdenId, String estado);
}
