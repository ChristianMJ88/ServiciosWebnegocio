package com.techprotech.agenda.modulos.citas.aplicacion;

import com.techprotech.agenda.compartido.correo.ServicioOutboxCorreoCitas;
import com.techprotech.agenda.compartido.whatsapp.ServicioOutboxWhatsappCitas;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Adaptador síncrono de registro: conserva el acuse real del outbox usado en el contrato público. */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class NotificacionesRegistroCita {
    private final ServicioOutboxCorreoCitas correo;
    private final ServicioOutboxWhatsappCitas whatsapp;
    public NotificacionesRegistroCita(ServicioOutboxCorreoCitas correo, ServicioOutboxWhatsappCitas whatsapp) {
        this.correo = correo; this.whatsapp = whatsapp;
    }
    public boolean registrada(Long empresaId, Long citaId, LocalDateTime inicio) {
        return correo.programarRegistrada(empresaId, citaId, inicio);
    }
    public void solicitarConfirmacion(Long empresaId, Long citaId, String telefono, LocalDateTime inicio) {
        whatsapp.programarConfirmacion(empresaId, citaId, telefono, inicio);
    }
    public void solicitarConfirmacion(Long empresaId, Long citaId, String telefono, LocalDateTime inicio, int cantidad) {
        whatsapp.programarConfirmacion(empresaId, citaId, telefono, inicio, cantidad);
    }
}
