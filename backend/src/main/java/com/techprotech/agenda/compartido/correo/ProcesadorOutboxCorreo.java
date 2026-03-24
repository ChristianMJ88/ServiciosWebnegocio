package com.techprotech.agenda.compartido.correo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.contactos.aplicacion.ServicioGestionSolicitudesContacto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProcesadorOutboxCorreo {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcesadorOutboxCorreo.class);
    private static final int MAX_INTENTOS = 5;

    private final BandejaSalidaNotificacionRepositorio bandejaSalidaNotificacionRepositorio;
    private final ServicioCorreoCitas servicioCorreoCitas;
    private final ServicioCorreoContactos servicioCorreoContactos;
    private final ServicioGestionSolicitudesContacto servicioGestionSolicitudesContacto;
    private final PropiedadesCorreo propiedadesCorreo;
    private final ObjectMapper objectMapper;

    public ProcesadorOutboxCorreo(
            BandejaSalidaNotificacionRepositorio bandejaSalidaNotificacionRepositorio,
            ServicioCorreoCitas servicioCorreoCitas,
            ServicioCorreoContactos servicioCorreoContactos,
            ServicioGestionSolicitudesContacto servicioGestionSolicitudesContacto,
            PropiedadesCorreo propiedadesCorreo,
            ObjectMapper objectMapper
    ) {
        this.bandejaSalidaNotificacionRepositorio = bandejaSalidaNotificacionRepositorio;
        this.servicioCorreoCitas = servicioCorreoCitas;
        this.servicioCorreoContactos = servicioCorreoContactos;
        this.servicioGestionSolicitudesContacto = servicioGestionSolicitudesContacto;
        this.propiedadesCorreo = propiedadesCorreo;
        this.objectMapper = objectMapper;
    }

    @Scheduled(
            fixedDelayString = "${aplicacion.correo.outbox.delay-ms:15000}",
            initialDelayString = "${aplicacion.correo.outbox.initial-delay-ms:10000}"
    )
    public void procesarPendientes() {
        List<BandejaSalidaNotificacionEntidad> pendientes = bandejaSalidaNotificacionRepositorio
                .reclamarPendientesEmail(propiedadesCorreo.loteMaximoOutbox(), LocalDateTime.now());

        for (BandejaSalidaNotificacionEntidad pendiente : pendientes) {
            procesar(pendiente);
        }
    }

    @Transactional
    protected void procesar(BandejaSalidaNotificacionEntidad pendiente) {
        try {
            procesarSegunEvento(pendiente);
            marcarComoEnviada(pendiente);
        } catch (JsonProcessingException ex) {
            LOGGER.warn("No se pudo leer el payload del outbox {}", pendiente.getId(), ex);
            marcarComoErrorFinal(pendiente, "Payload invalido para notificacion");
        } catch (Exception ex) {
            LOGGER.warn("Fallo el envio del outbox {}", pendiente.getId(), ex);
            reprogramar(pendiente, resumirError(ex));
        }
    }

    private void procesarSegunEvento(BandejaSalidaNotificacionEntidad pendiente) throws JsonProcessingException {
        if ("CITA_CONFIRMADA".equals(pendiente.getTipoEvento())) {
            ConfirmacionCitaCorreo confirmacion = objectMapper.readValue(pendiente.getPayloadJson(), ConfirmacionCitaCorreo.class);
            servicioCorreoCitas.enviarConfirmacion(pendiente.getEmpresaId(), confirmacion);
            return;
        }

        if ("CONTACTO_REGISTRADO".equals(pendiente.getTipoEvento())) {
            NotificacionSolicitudContactoCorreo notificacion = objectMapper.readValue(
                    pendiente.getPayloadJson(),
                    NotificacionSolicitudContactoCorreo.class
            );
            servicioCorreoContactos.enviarNotificacion(pendiente.getEmpresaId(), notificacion);
            servicioGestionSolicitudesContacto.marcarNotificada(notificacion.solicitudContactoId());
            return;
        }

        throw new IllegalStateException("Tipo de evento no soportado en outbox: " + pendiente.getTipoEvento());
    }

    private void marcarComoEnviada(BandejaSalidaNotificacionEntidad pendiente) {
        pendiente.setEstado("ENVIADA");
        pendiente.setEnviadaEn(LocalDateTime.now());
        pendiente.setMensajeError(null);
        pendiente.setIntentos(pendiente.getIntentos() + 1);
        bandejaSalidaNotificacionRepositorio.save(pendiente);
    }

    private void reprogramar(BandejaSalidaNotificacionEntidad pendiente, String error) {
        int nuevoIntento = pendiente.getIntentos() + 1;
        pendiente.setIntentos(nuevoIntento);
        pendiente.setMensajeError(error);

        if (nuevoIntento >= MAX_INTENTOS) {
            pendiente.setEstado("ERROR");
        } else {
            pendiente.setEstado("PENDIENTE");
            pendiente.setProgramadaEn(LocalDateTime.now().plusSeconds(propiedadesCorreo.retrasoReintentoSegundos()));
        }

        bandejaSalidaNotificacionRepositorio.save(pendiente);
    }

    private void marcarComoErrorFinal(BandejaSalidaNotificacionEntidad pendiente, String error) {
        pendiente.setEstado("ERROR");
        pendiente.setMensajeError(error);
        pendiente.setIntentos(pendiente.getIntentos() + 1);
        bandejaSalidaNotificacionRepositorio.save(pendiente);
    }

    private String resumirError(Exception ex) {
        String mensaje = ex.getMessage();
        if (mensaje == null || mensaje.isBlank()) {
            return "Error al enviar correo";
        }
        return mensaje.length() > 500 ? mensaje.substring(0, 500) : mensaje;
    }
}
