package com.techprotech.agenda.modulos.pagos.infraestructura.repositorio;

import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.EventoWebhookPagoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoWebhookPagoRepositorio extends JpaRepository<EventoWebhookPagoEntidad, Long> {
    boolean existsByProveedorPagoIdAndEventoExternoId(Long proveedorPagoId, String eventoExternoId);
}
