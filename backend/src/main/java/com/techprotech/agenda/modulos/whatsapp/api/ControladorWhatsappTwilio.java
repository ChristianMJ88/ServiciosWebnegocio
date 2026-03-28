package com.techprotech.agenda.modulos.whatsapp.api;

import com.techprotech.agenda.modulos.whatsapp.aplicacion.ServicioWhatsappCitas;
import com.techprotech.agenda.compartido.whatsapp.ServicioEstadoEntregaWhatsapp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/publico/whatsapp/twilio")
public class ControladorWhatsappTwilio {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControladorWhatsappTwilio.class);

    private final ServicioWhatsappCitas servicioWhatsappCitas;
    private final ServicioEstadoEntregaWhatsapp servicioEstadoEntregaWhatsapp;

    public ControladorWhatsappTwilio(
            ServicioWhatsappCitas servicioWhatsappCitas,
            ServicioEstadoEntregaWhatsapp servicioEstadoEntregaWhatsapp
    ) {
        this.servicioWhatsappCitas = servicioWhatsappCitas;
        this.servicioEstadoEntregaWhatsapp = servicioEstadoEntregaWhatsapp;
    }

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> recibir(@RequestParam MultiValueMap<String, String> params) {
        String from = params.getFirst("From");
        String body = resolverMensajeEntrante(params);

        String respuesta;
        try {
            respuesta = servicioWhatsappCitas.procesarMensaje(from, body);
        } catch (Exception ex) {
            LOGGER.error("No se pudo procesar el webhook de WhatsApp", ex);
            respuesta = "Ocurrio un error al procesar tu mensaje. Intenta nuevamente con AYUDA.";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body("""
                        <Response>
                          <Message>%s</Message>
                        </Response>
                        """.formatted(escaparXml(respuesta)));
    }

    @PostMapping(value = "/status", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> recibirEstado(@RequestParam MultiValueMap<String, String> params) {
        try {
            servicioEstadoEntregaWhatsapp.registrarActualizacion(
                    params.getFirst("MessageSid"),
                    params.getFirst("MessageStatus"),
                    params.getFirst("ErrorCode"),
                    params.getFirst("ErrorMessage")
            );
        } catch (Exception ex) {
            LOGGER.error("No se pudo procesar el callback de estado de WhatsApp", ex);
        }

        return ResponseEntity.ok().build();
    }

    private String escaparXml(String texto) {
        return texto
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String resolverMensajeEntrante(MultiValueMap<String, String> params) {
        String buttonPayload = params.getFirst("ButtonPayload");
        if (buttonPayload != null && !buttonPayload.isBlank()) {
            return switch (buttonPayload.trim()) {
                case "QUIERO_AGENDAR", "AGENDAR_DESDE_RECORDATORIO" -> "Quiero agendar";
                case "MIS_CITAS" -> "MIS CITAS";
                case "CONFIRMAR_CITA" -> "CONFIRMAR";
                case "VER_SERVICIOS" -> "Ver servicios";
                case "VER_HORARIOS" -> "Horarios disponibles";
                case "VER_UBICACION" -> "Ubicación";
                case "VER_PROMOCIONES" -> "Promociones";
                case "PAUSAR_RECORDATORIOS" -> "Pausar recordatorios";
                case "NO_POR_AHORA" -> "No por ahora";
                default -> buttonPayload;
            };
        }

        String buttonText = params.getFirst("ButtonText");
        if (buttonText != null && !buttonText.isBlank()) {
            return buttonText;
        }

        return params.getFirst("Body");
    }
}
