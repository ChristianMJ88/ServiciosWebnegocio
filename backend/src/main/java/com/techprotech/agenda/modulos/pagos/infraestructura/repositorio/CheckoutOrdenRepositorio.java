package com.techprotech.agenda.modulos.pagos.infraestructura.repositorio;

import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CheckoutOrdenRepositorio extends JpaRepository<CheckoutOrdenEntidad, Long> {
    Optional<CheckoutOrdenEntidad> findByEmpresaIdAndIdempotencia(Long empresaId, String idempotencia);
    Optional<CheckoutOrdenEntidad> findByEmpresaIdAndPublicId(Long empresaId, String publicId);
}
