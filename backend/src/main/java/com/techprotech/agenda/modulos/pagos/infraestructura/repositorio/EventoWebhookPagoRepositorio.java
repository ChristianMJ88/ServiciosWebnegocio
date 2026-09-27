package com.techprotech.agenda.modulos.pagos.infraestructura.repositorio;

import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.EventoWebhookPagoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoWebhookPagoRepositorio extends JpaRepository<EventoWebhookPagoEntidad, Long> {
    boolean existsByProveedorPagoIdAndEventoExternoId(Long proveedorPagoId, String eventoExternoId);
    List<EventoWebhookPagoEntidad> findTop50ByEstadoProcesamientoOrderByRecibidoEnAsc(String estadoProcesamiento);
}
