package com.techprotech.agenda.modulos.whatsapp.api;

import com.techprotech.agenda.modulos.whatsapp.aplicacion.ServicioWhatsappCitas;
import com.techprotech.agenda.compartido.whatsapp.ServicioEstadoEntregaWhatsapp;
import com.techprotech.agenda.compartido.whatsapp.ConfiguracionWhatsappEmpresaEntidad;
import com.techprotech.agenda.compartido.whatsapp.ResolutorEmpresaWebhookWhatsapp;
import com.techprotech.agenda.compartido.whatsapp.ValidadorFirmaWebhookTwilio;
import jakarta.servlet.http.HttpServletRequest;
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
    private final ResolutorEmpresaWebhookWhatsapp resolutorEmpresaWebhookWhatsapp;
    private final ValidadorFirmaWebhookTwilio validadorFirmaWebhookTwilio;

    public ControladorWhatsappTwilio(
            ServicioWhatsappCitas servicioWhatsappCitas,
            ServicioEstadoEntregaWhatsapp servicioEstadoEntregaWhatsapp,
            ResolutorEmpresaWebhookWhatsapp resolutorEmpresaWebhookWhatsapp,
            ValidadorFirmaWebhookTwilio validadorFirmaWebhookTwilio
    ) {
        this.servicioWhatsappCitas = servicioWhatsappCitas;
        this.servicioEstadoEntregaWhatsapp = servicioEstadoEntregaWhatsapp;
        this.resolutorEmpresaWebhookWhatsapp = resolutorEmpresaWebhookWhatsapp;
        this.validadorFirmaWebhookTwilio = validadorFirmaWebhookTwilio;
    }

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> recibir(
            HttpServletRequest request,
            @RequestParam MultiValueMap<String, String> params
    ) {
        ConfiguracionWhatsappEmpresaEntidad configuracion = resolutorEmpresaWebhookWhatsapp.resolver(params);
        Long empresaId = configuracion.getEmpresaId();
        validadorFirmaWebhookTwilio.validar(empresaId, request, params, request.getHeader("X-Twilio-Signature"));
        String from = params.getFirst("From");
        String body = resolverMensajeEntrante(params);
        servicioWhatsappCitas.registrarMensajeEntrante(empresaId, from, resolverMensajeVisibleEntrante(params, body));

        ServicioWhatsappCitas.RespuestaWhatsapp respuesta;
        try {
            respuesta = servicioWhatsappCitas.procesarWebhook(empresaId, from, body);
        } catch (Exception ex) {
            LOGGER.error("No se pudo procesar el webhook de WhatsApp", ex);
            respuesta = ServicioWhatsappCitas.RespuestaWhatsapp.texto(
                    "Ocurrio un error al procesar tu mensaje. Intenta nuevamente con AYUDA."
            );
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(construirRespuestaTwiml(empresaId, from, respuesta));
    }

    @PostMapping(value = "/status", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> recibirEstado(
            HttpServletRequest request,
            @RequestParam MultiValueMap<String, String> params
    ) {
        ConfiguracionWhatsappEmpresaEntidad configuracion = resolutorEmpresaWebhookWhatsapp.resolver(params);
        validadorFirmaWebhookTwilio.validar(
                configuracion.getEmpresaId(), request, params, request.getHeader("X-Twilio-Signature")
        );
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

    private String construirRespuestaTwiml(Long empresaId, String telefonoRemitente, ServicioWhatsappCitas.RespuestaWhatsapp respuesta) {
        if (respuesta == null) {
            return "<Response></Response>";
        }

        if (respuesta.tieneContenidoInteractivo() && servicioWhatsappCitas.enviarContenidoInteractivo(empresaId, telefonoRemitente, respuesta)) {
            return "<Response></Response>";
        }

        if (respuesta.mensaje() == null || respuesta.mensaje().isBlank()) {
            return "<Response></Response>";
        }

        servicioWhatsappCitas.registrarMensajeSalienteTexto(empresaId, telefonoRemitente, respuesta.mensaje());
        return """
                <Response>
                  <Message>%s</Message>
                </Response>
                """.formatted(escaparXml(respuesta.mensaje()));
    }

    private String resolverMensajeEntrante(MultiValueMap<String, String> params) {
        String buttonPayload = params.getFirst("ButtonPayload");
        if (buttonPayload != null && !buttonPayload.isBlank()) {
            String normalizado = normalizarAccionRapida(buttonPayload);
            return normalizado != null ? normalizado : buttonPayload.trim();
        }

        String buttonText = params.getFirst("ButtonText");
        if (buttonText != null && !buttonText.isBlank()) {
            String normalizado = normalizarAccionRapida(buttonText);
            return normalizado != null ? normalizado : buttonText.trim();
        }

        return params.getFirst("Body");
    }

    private String resolverMensajeVisibleEntrante(MultiValueMap<String, String> params, String fallback) {
        String buttonText = params.getFirst("ButtonText");
        if (buttonText != null && !buttonText.isBlank()) {
            return buttonText.trim();
        }

        String body = params.getFirst("Body");
        if (body != null && !body.isBlank()) {
            return body.trim();
        }

        String buttonPayload = params.getFirst("ButtonPayload");
        if (buttonPayload != null && !buttonPayload.isBlank()) {
            return buttonPayload.trim();
        }

        return fallback;
    }

    private String normalizarAccionRapida(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        String limpio = valor.trim();
        String canonico = limpio
                .toUpperCase()
                .replace('Á', 'A')
                .replace('É', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Ú', 'U');

        return switch (canonico) {
            case "QUIERO_AGENDAR", "AGENDAR_DESDE_RECORDATORIO" -> "Quiero agendar";
            case "MENU_AGENDAR", "MENU_AGENDAR_CITA", "MENU_CITA" -> "Quiero agendar";
            case "MIS_CITAS", "MIS CITAS" -> "MIS CITAS";
            case "MENU_MIS_CITAS" -> "MIS CITAS";
            case "VER_DETALLE", "VER DETALLE", "VER DETALLES" -> "VER DETALLE";
            case "CONFIRMAR_CITA", "CONFIRMAR CITA" -> "CONFIRMAR CITA";
            case "VER_SERVICIOS", "VER SERVICIOS" -> "Ver servicios";
            case "MENU_SERVICIOS" -> "Ver servicios";
            case "VER_HORARIOS", "HORARIOS DISPONIBLES" -> "Horarios disponibles";
            case "MENU_HORARIOS" -> "Horarios disponibles";
            case "VER_UBICACION", "UBICACION" -> "Ubicación";
            case "MENU_UBICACION" -> "Ubicación";
            case "VER_PROMOCIONES", "PROMOCIONES" -> "Promociones";
            case "MENU_PROMOCIONES" -> "Promociones";
            case "PAUSAR_RECORDATORIOS" -> "Pausar recordatorios";
            case "NO_POR_AHORA" -> "No por ahora";
            default -> null;
        };
    }
}
