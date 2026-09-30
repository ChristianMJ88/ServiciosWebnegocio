package com.techprotech.agenda.modulos.envios.infraestructura.repositorio;

import com.techprotech.agenda.modulos.envios.infraestructura.entidad.EventoWebhookEnvioEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoWebhookEnvioRepositorio extends JpaRepository<EventoWebhookEnvioEntidad, Long> {
    boolean existsByEmpresaIdAndProveedorAndEventoExternoId(Long empresaId, String proveedor, String eventoExternoId);
    List<EventoWebhookEnvioEntidad> findTop50ByEstadoProcesamientoOrderByRecibidoEnAsc(String estadoProcesamiento);
}
