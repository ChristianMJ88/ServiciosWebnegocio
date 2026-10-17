package com.techprotech.agenda.modulos.citas.aplicacion;

import com.techprotech.agenda.compartido.correo.ServicioOutboxCorreoCitas;
import com.techprotech.agenda.compartido.whatsapp.ServicioOutboxWhatsappCitas;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import java.time.Clock;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/** Wiring de unidad con el publicador y @EventListener reales; los proveedores siguen simulados. */
public final class EventosCitaPruebas {
    private EventosCitaPruebas() {}
    public static PublicadorEventosCita publicador(ServicioOutboxCorreoCitas correo,
            ServicioOutboxWhatsappCitas whatsapp, ClienteRepositorio clientes, Clock reloj) {
        var contexto = new AnnotationConfigApplicationContext();
        contexto.registerBean(NotificacionesEventosCita.class, () -> new NotificacionesEventosCita(correo, whatsapp, clientes));
        contexto.refresh();
        return new PublicadorEventosCita(contexto, reloj);
    }
}
