package com.techprotech.agenda.modulos.citas.aplicacion;

import com.techprotech.agenda.modulos.citas.dominio.eventos.*;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import java.time.Clock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class PublicadorEventosCita {
    private final ApplicationEventPublisher publicador;
    private final Clock reloj;

    public PublicadorEventosCita(ApplicationEventPublisher publicador, Clock reloj) {
        this.publicador = publicador;
        this.reloj = reloj;
    }

    public DatosEventoCita datos(CitaEntidad cita, OrigenEventoCita origen, Long actorId) {
        return new DatosEventoCita(cita.getEmpresaId(), cita.getSucursalId(), cita.getId(), cita.getClienteId(),
                origen, actorId, reloj.instant(), cita.getInicio(), cita.getFin());
    }

    public void publicar(EventoCita evento) { publicador.publishEvent(evento); }
}
