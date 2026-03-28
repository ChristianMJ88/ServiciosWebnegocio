package com.techprotech.agenda.compartido.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.compartido.correo.BandejaSalidaNotificacionEntidad;
import com.techprotech.agenda.compartido.correo.BandejaSalidaNotificacionRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProcesadorOutboxWhatsapp {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcesadorOutboxWhatsapp.class);
    private static final int MAX_INTENTOS = 5;

    private final BandejaSalidaNotificacionRepositorio bandejaSalidaNotificacionRepositorio;
    private final ServicioMensajesWhatsappCitas servicioMensajesWhatsappCitas;
    private final PropiedadesWhatsapp propiedadesWhatsapp;
    private final ObjectMapper objectMapper;

    public ProcesadorOutboxWhatsapp(
            BandejaSalidaNotificacionRepositorio bandejaSalidaNotificacionRepositorio,
            ServicioMensajesWhatsappCitas servicioMensajesWhatsappCitas,
            PropiedadesWhatsapp propiedadesWhatsapp,
            ObjectMapper objectMapper
    ) {
        this.bandejaSalidaNotificacionRepositorio = bandejaSalidaNotificacionRepositorio;
        this.servicioMensajesWhatsappCitas = servicioMensajesWhatsappCitas;
        this.propiedadesWhatsapp = propiedadesWhatsapp;
        this.objectMapper = objectMapper;
    }

    @Scheduled(
            fixedDelayString = "${aplicacion.whatsapp.outbox.delay-ms:15000}",
            initialDelayString = "${aplicacion.whatsapp.outbox.initial-delay-ms:12000}"
    )
    public void procesarPendientes() {
        List<BandejaSalidaNotificacionEntidad> pendientes = bandejaSalidaNotificacionRepositorio
                .reclamarPendientesPorCanal("WHATSAPP", propiedadesWhatsapp.loteMaximoOutbox(), LocalDateTime.now());

        for (BandejaSalidaNotificacionEntidad pendiente : pendientes) {
            procesar(pendiente);
        }
    }

    @Transactional
    protected void procesar(BandejaSalidaNotificacionEntidad pendiente) {
        try {
            MensajeCitaWhatsappPayload payload = objectMapper.readValue(pendiente.getPayloadJson(), MensajeCitaWhatsappPayload.class);
            ResultadoEnvioWhatsapp resultado = servicioMensajesWhatsappCitas.enviarNotificacion(
                    pendiente.getEmpresaId(),
                    pendiente.getTipoEvento(),
                    payload
            );
            marcarComoEnviada(pendiente, resultado);
        } catch (JsonProcessingException ex) {
            LOGGER.warn("No se pudo leer el payload de WhatsApp {}", pendiente.getId(), ex);
            marcarComoErrorFinal(pendiente);
        } catch (Exception ex) {
            LOGGER.warn("Fallo el envio de WhatsApp {}", pendiente.getId(), ex);
            reprogramar(pendiente, resumirError(ex));
        }
    }

    private void marcarComoEnviada(BandejaSalidaNotificacionEntidad pendiente, ResultadoEnvioWhatsapp resultado) {
        pendiente.setEstado("ENVIADA");
        pendiente.setEnviadaEn(LocalDateTime.now());
        pendiente.setMensajeError(null);
        pendiente.setIntentos(pendiente.getIntentos() + 1);
        if (resultado != null) {
            pendiente.setProveedorMensajeId(resultado.proveedorMensajeId());
            pendiente.setEstadoEntrega(resultado.estadoProveedor());
            pendiente.setEstadoEntregaActualizadoEn(LocalDateTime.now());
            pendiente.setCodigoErrorProveedor(resultado.codigoErrorProveedor());
            pendiente.setDetalleErrorProveedor(resultado.detalleErrorProveedor());
        }
        bandejaSalidaNotificacionRepositorio.save(pendiente);
        LOGGER.info(
                "WhatsApp enviado para outbox {} cita {} evento {} sid {} estado {}",
                pendiente.getId(),
                pendiente.getAgregadoId(),
                pendiente.getTipoEvento(),
                resultado != null ? resultado.proveedorMensajeId() : "N/A",
                resultado != null ? resultado.estadoProveedor() : "N/A"
        );
    }

    private void reprogramar(BandejaSalidaNotificacionEntidad pendiente, String error) {
        int nuevoIntento = pendiente.getIntentos() + 1;
        pendiente.setIntentos(nuevoIntento);
        pendiente.setMensajeError(error);

        if (nuevoIntento >= MAX_INTENTOS) {
            pendiente.setEstado("ERROR");
        } else {
            pendiente.setEstado("PENDIENTE");
            pendiente.setProgramadaEn(LocalDateTime.now().plusSeconds(propiedadesWhatsapp.retrasoReintentoSegundos()));
        }

        bandejaSalidaNotificacionRepositorio.save(pendiente);
    }

    private void marcarComoErrorFinal(BandejaSalidaNotificacionEntidad pendiente) {
        pendiente.setEstado("ERROR");
        pendiente.setMensajeError("Payload invalido para WhatsApp");
        pendiente.setIntentos(pendiente.getIntentos() + 1);
        bandejaSalidaNotificacionRepositorio.save(pendiente);
    }

    private String resumirError(Exception ex) {
        String mensaje = ex.getMessage();
        if (mensaje == null || mensaje.isBlank()) {
            return "Error al enviar WhatsApp";
        }
        return mensaje.length() > 500 ? mensaje.substring(0, 500) : mensaje;
    }
}
