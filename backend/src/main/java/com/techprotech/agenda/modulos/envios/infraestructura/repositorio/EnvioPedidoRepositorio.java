package com.techprotech.agenda.modulos.envios.infraestructura.repositorio;

import com.techprotech.agenda.modulos.envios.infraestructura.entidad.EnvioPedidoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnvioPedidoRepositorio extends JpaRepository<EnvioPedidoEntidad, Long> {
    Optional<EnvioPedidoEntidad> findByEmpresaIdAndCheckoutOrdenId(Long empresaId, Long checkoutOrdenId);
    List<EnvioPedidoEntidad> findTop20ByEmpresaIdOrderByCreadoEnDesc(Long empresaId);
}
