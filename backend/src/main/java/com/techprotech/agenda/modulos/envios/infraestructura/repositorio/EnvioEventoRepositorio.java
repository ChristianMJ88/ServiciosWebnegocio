package com.techprotech.agenda.modulos.envios.infraestructura.repositorio;

import com.techprotech.agenda.modulos.envios.infraestructura.entidad.EnvioEventoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnvioEventoRepositorio extends JpaRepository<EnvioEventoEntidad, Long> {
    List<EnvioEventoEntidad> findByEmpresaIdAndEnvioPedidoIdOrderByCreadoEnAscIdAsc(Long empresaId, Long envioPedidoId);
    boolean existsByEmpresaIdAndReferenciaExterna(Long empresaId, String referenciaExterna);
}
