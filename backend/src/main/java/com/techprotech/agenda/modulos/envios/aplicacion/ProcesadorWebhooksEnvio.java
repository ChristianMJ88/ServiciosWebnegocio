package com.techprotech.agenda.modulos.envios.aplicacion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.envios.infraestructura.entidad.EventoWebhookEnvioEntidad;
import com.techprotech.agenda.modulos.envios.infraestructura.repositorio.EventoWebhookEnvioRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class ProcesadorWebhooksEnvio {
    private final java.time.Clock reloj;
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcesadorWebhooksEnvio.class);
    private static final int MAX_INTENTOS = 5;
    private final EventoWebhookEnvioRepositorio eventos;
    private final ServicioGuiasEnvio guias;
    private final ObjectMapper objectMapper;

    public ProcesadorWebhooksEnvio(EventoWebhookEnvioRepositorio eventos, ServicioGuiasEnvio guias,
                                   ObjectMapper objectMapper, java.time.Clock reloj) {
        this.reloj = reloj;
        this.eventos = eventos;
        this.guias = guias;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${aplicacion.envios.intervalo-webhooks-ms:5000}")
    public void procesarPendientes() {
        eventos.findTop50ByEstadoProcesamientoOrderByRecibidoEnAsc("RECIBIDO").forEach(this::procesar);
    }

    @Transactional
    public void procesar(EventoWebhookEnvioEntidad evento) {
        try {
            JsonNode cuerpo = objectMapper.readTree(evento.getPayload());
            JsonNode data = cuerpo.path("data");
            if (data.isMissingNode() || !data.isObject()) data = cuerpo;
            String numeroGuia = primero(texto(data, "tracking_number"), texto(data, "trackingNumber"));
            String estado = primero(texto(data, "status"), texto(data, "shipment_status"));
            boolean rastreo = "tracking.simple".equalsIgnoreCase(evento.getTipo())
                    || "simpleTracking".equalsIgnoreCase(evento.getTipo())
                    || (numeroGuia != null && estado != null);
            if (!rastreo) {
                evento.setEstadoProcesamiento("IGNORADO");
            } else {
                guias.aplicarEventoWebhook(evento.getEmpresaId(), evento.getProveedor(),
                        primero(texto(data, "shipment_id"), texto(data, "shipmentId")), numeroGuia,
                        estado, primero(texto(data, "status_description"), texto(data, "description")),
                        evento.getEventoExternoId());
                evento.setEstadoProcesamiento("PROCESADO");
            }
            evento.setProcesadoEn(LocalDateTime.now(reloj));
            evento.setErrorUltimo(null);
        } catch (Exception ex) {
            int intentos = evento.getIntentos() + 1;
            evento.setIntentos(intentos);
            evento.setErrorUltimo(resumir(ex));
            if (intentos >= MAX_INTENTOS) evento.setEstadoProcesamiento("ERROR");
            LOGGER.warn("No fue posible procesar el webhook de envío {}", evento.getEventoExternoId(), ex);
        }
        eventos.save(evento);
    }

    private String texto(JsonNode nodo, String campo) {
        JsonNode valor = nodo.path(campo);
        return valor.isMissingNode() || valor.isNull() ? null : valor.asText();
    }

    private String primero(String primero, String segundo) {
        return primero == null || primero.isBlank() ? segundo : primero;
    }

    private String resumir(Exception ex) {
        String mensaje = ex.getMessage();
        if (mensaje == null || mensaje.isBlank()) return "Error al procesar el evento de rastreo";
        return mensaje.length() > 1000 ? mensaje.substring(0, 1000) : mensaje;
    }
}
