package com.techprotech.agenda.modulos.pagos.infraestructura.repositorio;

import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenDetalleEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CheckoutOrdenDetalleRepositorio extends JpaRepository<CheckoutOrdenDetalleEntidad, Long> {
    List<CheckoutOrdenDetalleEntidad> findByEmpresaIdAndCheckoutOrdenIdOrderByIdAsc(Long empresaId, Long checkoutOrdenId);
}
