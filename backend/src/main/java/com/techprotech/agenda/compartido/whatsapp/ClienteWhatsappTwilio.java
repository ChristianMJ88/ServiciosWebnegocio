package com.techprotech.agenda.compartido.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
public class ClienteWhatsappTwilio {

    private final ServicioConfiguracionWhatsappEmpresa servicioConfiguracionWhatsappEmpresa;
    private final PropiedadesWhatsapp propiedadesWhatsapp;
    private final RestClient restClient;
    private final RestClient messagingRestClient;
    private final RestClient contentRestClient;
    private final ObjectMapper objectMapper;

    public ClienteWhatsappTwilio(
            ServicioConfiguracionWhatsappEmpresa servicioConfiguracionWhatsappEmpresa,
            PropiedadesWhatsapp propiedadesWhatsapp,
            ObjectMapper objectMapper
    ) {
        this.servicioConfiguracionWhatsappEmpresa = servicioConfiguracionWhatsappEmpresa;
        this.propiedadesWhatsapp = propiedadesWhatsapp;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.twilio.com/2010-04-01")
                .build();
        this.messagingRestClient = RestClient.builder()
                .baseUrl("https://messaging.twilio.com/v1")
                .build();
        this.contentRestClient = RestClient.builder()
                .baseUrl("https://content.twilio.com")
                .build();
    }

    public boolean estaHabilitado(Long empresaId) {
        return servicioConfiguracionWhatsappEmpresa.resolver(empresaId).habilitado();
    }

    public String diagnosticoConfiguracion(Long empresaId) {
        ConfiguracionWhatsappResolvida configuracion = servicioConfiguracionWhatsappEmpresa.resolver(empresaId);
        List<String> faltantes = new ArrayList<>();
        if (!configuracion.habilitado()) {
            faltantes.add("WHATSAPP_HABILITADO=false");
        }
        if (!tieneTexto(configuracion.accountSid())) {
            faltantes.add("falta TWILIO_ACCOUNT_SID");
        }
        if (!tieneTexto(configuracion.authToken())) {
            faltantes.add("falta TWILIO_AUTH_TOKEN");
        }
        if (!tieneTexto(configuracion.numeroRemitente())) {
            faltantes.add("falta TWILIO_WHATSAPP_FROM");
        }
        return faltantes.isEmpty() ? "OK" : String.join(", ", faltantes);
    }

    public ResultadoEnvioWhatsapp enviarMensaje(Long empresaId, String telefonoDestino, String mensaje) {
        ConfiguracionWhatsappResolvida configuracion = servicioConfiguracionWhatsappEmpresa.resolver(empresaId);
        if (!configuracion.habilitado()) {
            throw new IllegalStateException("Twilio WhatsApp no esta configurado: " + diagnosticoConfiguracion(empresaId));
        }

        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("From", NormalizadorTelefonoWhatsapp.aDireccionWhatsapp(configuracion.numeroRemitente()));
        formulario.add("To", NormalizadorTelefonoWhatsapp.aDireccionWhatsapp(telefonoDestino));
        formulario.add("Body", mensaje);
        return enviarFormulario(configuracion, formulario);
    }

    public ResultadoEnvioWhatsapp enviarPlantilla(Long empresaId, String telefonoDestino, String contentSid, Map<String, String> variables) {
        ConfiguracionWhatsappResolvida configuracion = servicioConfiguracionWhatsappEmpresa.resolver(empresaId);
        if (!configuracion.habilitado()) {
            throw new IllegalStateException("Twilio WhatsApp no esta configurado: " + diagnosticoConfiguracion(empresaId));
        }
        if (!tieneTexto(contentSid)) {
            throw new IllegalArgumentException("El ContentSid de la plantilla es obligatorio");
        }
        if (!tieneTexto(configuracion.messagingServiceSid())) {
            throw new IllegalStateException("Falta configurar TWILIO_MESSAGING_SERVICE_SID para enviar plantillas de WhatsApp");
        }

        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("From", NormalizadorTelefonoWhatsapp.aDireccionWhatsapp(configuracion.numeroRemitente()));
        formulario.add("To", NormalizadorTelefonoWhatsapp.aDireccionWhatsapp(telefonoDestino));
        formulario.add("MessagingServiceSid", configuracion.messagingServiceSid());
        formulario.add("ContentSid", contentSid);
        formulario.add("ContentVariables", serializarVariables(variables));
        return enviarFormulario(configuracion, formulario);
    }

    public List<PlantillaTwilioWhatsapp> listarPlantillasWhatsapp(Long empresaId) {
        ConfiguracionWhatsappResolvida configuracion = servicioConfiguracionWhatsappEmpresa.resolver(empresaId);
        if (!tieneTexto(configuracion.accountSid()) || !tieneTexto(configuracion.authToken())) {
            throw new IllegalStateException("No hay credenciales de Twilio configuradas para consultar plantillas");
        }

        List<PlantillaTwilioWhatsapp> plantillas = new ArrayList<>();
        String siguienteUrl = "/v1/ContentAndApprovals?PageSize=100";

        while (tieneTexto(siguienteUrl)) {
            JsonNode respuesta = contentRestClient.get()
                    .uri(siguienteUrl)
                    .headers(headers -> headers.setBasicAuth(configuracion.accountSid(), configuracion.authToken()))
                    .retrieve()
                    .body(JsonNode.class);

            if (respuesta == null) {
                break;
            }

            JsonNode contenidos = respuesta.path("contents");
            if (contenidos.isArray()) {
                for (JsonNode contenido : contenidos) {
                    if (esPlantillaWhatsapp(contenido, configuracion)) {
                        plantillas.add(mapearPlantilla(contenido));
                    }
                }
            }

            siguienteUrl = extraerSiguienteUrl(respuesta.path("meta").path("next_page_url"));
        }

        return plantillas;
    }

    public SubcuentaTwilioProvisionada provisionarSubcuenta(String friendlyName) {
        if (!tieneTexto(propiedadesWhatsapp.accountSid()) || !tieneTexto(propiedadesWhatsapp.authToken())) {
            throw new IllegalStateException("Faltan credenciales de la cuenta principal de Twilio para crear subcuentas");
        }

        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        if (tieneTexto(friendlyName)) {
            formulario.add("FriendlyName", friendlyName);
        }

        RespuestaSubcuentaTwilio respuesta = restClient.post()
                .uri("/Accounts.json")
                .headers(headers -> headers.setBasicAuth(propiedadesWhatsapp.accountSid(), propiedadesWhatsapp.authToken()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formulario)
                .retrieve()
                .body(RespuestaSubcuentaTwilio.class);

        if (respuesta == null || !tieneTexto(respuesta.sid()) || !tieneTexto(respuesta.authToken())) {
            throw new IllegalStateException("Twilio no devolvio la subcuenta creada");
        }

        return new SubcuentaTwilioProvisionada(
                respuesta.sid(),
                respuesta.authToken(),
                respuesta.friendlyName(),
                respuesta.ownerAccountSid(),
                respuesta.status()
        );
    }

    public MessagingServiceProvisionado provisionarMessagingService(
            Long empresaId,
            String friendlyName,
            String inboundRequestUrl,
            String statusCallbackUrl
    ) {
        ConfiguracionWhatsappResolvida configuracion = servicioConfiguracionWhatsappEmpresa.resolver(empresaId);
        if (!tieneTexto(configuracion.accountSid()) || !tieneTexto(configuracion.authToken())) {
            throw new IllegalStateException("Faltan credenciales de Twilio del tenant para crear el Messaging Service");
        }

        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("FriendlyName", friendlyName);
        if (tieneTexto(inboundRequestUrl)) {
            formulario.add("InboundRequestUrl", inboundRequestUrl);
            formulario.add("InboundMethod", "POST");
        }
        if (tieneTexto(statusCallbackUrl)) {
            formulario.add("StatusCallback", statusCallbackUrl);
        }

        RespuestaMessagingServiceTwilio respuesta = messagingRestClient.post()
                .uri("/Services")
                .headers(headers -> headers.setBasicAuth(configuracion.accountSid(), configuracion.authToken()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formulario)
                .retrieve()
                .body(RespuestaMessagingServiceTwilio.class);

        if (respuesta == null || !tieneTexto(respuesta.sid())) {
            throw new IllegalStateException("Twilio no devolvio el Messaging Service creado");
        }

        return new MessagingServiceProvisionado(
                respuesta.sid(),
                respuesta.friendlyName(),
                respuesta.inboundRequestUrl(),
                respuesta.statusCallback(),
                respuesta.accountSid()
        );
    }

    public void asociarChannelSenderAMessagingService(Long empresaId, String messagingServiceSid, String channelSenderSid) {
        ConfiguracionWhatsappResolvida configuracion = servicioConfiguracionWhatsappEmpresa.resolver(empresaId);
        if (!tieneTexto(configuracion.accountSid()) || !tieneTexto(configuracion.authToken())) {
            throw new IllegalStateException("Faltan credenciales de Twilio del tenant para asociar el sender");
        }
        if (!tieneTexto(messagingServiceSid)) {
            throw new IllegalArgumentException("El Messaging Service SID es obligatorio");
        }
        if (!tieneTexto(channelSenderSid)) {
            throw new IllegalArgumentException("El Channel Sender SID es obligatorio");
        }

        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("ChannelSenderSid", channelSenderSid);

        messagingRestClient.post()
                .uri("/Services/{messagingServiceSid}/ChannelSenders", messagingServiceSid)
                .headers(headers -> headers.setBasicAuth(configuracion.accountSid(), configuracion.authToken()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formulario)
                .retrieve()
                .toBodilessEntity();
    }

    public SenderTwilioWhatsapp detectarSenderWhatsapp(Long empresaId, String numeroRemitentePreferido) {
        ConfiguracionWhatsappResolvida configuracion = servicioConfiguracionWhatsappEmpresa.resolver(empresaId);
        if (!tieneTexto(configuracion.accountSid()) || !tieneTexto(configuracion.authToken())) {
            throw new IllegalStateException("Faltan credenciales de Twilio del tenant para consultar senders");
        }

        String senderObjetivo = tieneTexto(numeroRemitentePreferido)
                ? numeroRemitentePreferido
                : configuracion.numeroRemitente();

        List<SenderTwilioWhatsapp> senders = listarSendersWhatsapp(configuracion);
        if (!tieneTexto(senderObjetivo)) {
            return senders.isEmpty() ? null : senders.get(0);
        }

        return senders.stream()
                .filter(sender -> NormalizadorTelefonoWhatsapp.coincide(sender.senderId(), senderObjetivo))
                .findFirst()
                .orElse(null);
    }

    private List<SenderTwilioWhatsapp> listarSendersWhatsapp(ConfiguracionWhatsappResolvida configuracion) {
        List<SenderTwilioWhatsapp> senders = new ArrayList<>();
        String siguienteUrl = "/Channels/Senders?Channel=whatsapp&PageSize=100";

        while (tieneTexto(siguienteUrl)) {
            JsonNode respuesta = messagingRestClient.get()
                    .uri(siguienteUrl)
                    .headers(headers -> headers.setBasicAuth(configuracion.accountSid(), configuracion.authToken()))
                    .retrieve()
                    .body(JsonNode.class);

            if (respuesta == null) {
                break;
            }

            JsonNode items = respuesta.path("senders");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    senders.add(new SenderTwilioWhatsapp(
                            item.path("sid").asText(null),
                            item.path("sender_id").asText(null),
                            item.path("status").asText(null),
                            item.path("profile").path("name").asText(null),
                            item.path("configuration").path("waba_id").asText(null)
                    ));
                }
            }

            siguienteUrl = extraerSiguienteUrlBase(
                    respuesta.path("meta").path("next_page_url"),
                    "https://messaging.twilio.com/v2"
            );
        }

        return senders;
    }

    private ResultadoEnvioWhatsapp enviarFormulario(ConfiguracionWhatsappResolvida configuracion, MultiValueMap<String, String> formulario) {
        if (tieneTexto(configuracion.statusCallbackUrl())) {
            formulario.add("StatusCallback", configuracion.statusCallbackUrl());
        }

        RespuestaTwilio respuesta = restClient.post()
                .uri("/Accounts/{accountSid}/Messages.json", configuracion.accountSid())
                .headers(headers -> headers.setBasicAuth(configuracion.accountSid(), configuracion.authToken()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formulario)
                .retrieve()
                .body(RespuestaTwilio.class);

        if (respuesta == null) {
            return new ResultadoEnvioWhatsapp(null, null, null, null);
        }

        return new ResultadoEnvioWhatsapp(
                respuesta.sid(),
                respuesta.status(),
                respuesta.errorCode(),
                respuesta.errorMessage()
        );
    }

    private String serializarVariables(Map<String, String> variables) {
        try {
            return objectMapper.writeValueAsString(variables);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudieron serializar las variables de la plantilla de WhatsApp", ex);
        }
    }

    private boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private boolean esPlantillaWhatsapp(JsonNode contenido, ConfiguracionWhatsappResolvida configuracion) {
        JsonNode approvals = contenido.path("approvals");
        if (approvals.isObject() && approvals.has("whatsapp")) {
            return true;
        }

        String sid = contenido.path("sid").asText(null);
        return tieneTexto(sid)
                && (
                sid.equals(configuracion.plantillaSolicitudConfirmacionSid())
                        || sid.equals(configuracion.plantillaReprogramadaPendienteSid())
                        || sid.equals(configuracion.plantillaRecordatorioConfirmacionSid())
                        || sid.equals(configuracion.plantillaCitaConfirmadaSid())
                        || sid.equals(configuracion.plantillaRecordatorioSid())
                        || sid.equals(configuracion.plantillaCancelacionSid())
                        || sid.equals(configuracion.plantillaLiberadaSinConfirmacionSid())
                        || sid.equals(configuracion.plantillaGraciasVisitaSid())
                        || sid.equals(configuracion.plantillaRecordatorioRegresoSid())
        );
    }

    private PlantillaTwilioWhatsapp mapearPlantilla(JsonNode contenido) {
        String sid = contenido.path("sid").asText(null);
        String nombre = contenido.path("friendly_name").asText(null);
        String idioma = contenido.path("language").asText(null);
        JsonNode approvalsWhatsapp = contenido.path("approvals").path("whatsapp");
        String categoria = approvalsWhatsapp.path("category").asText(null);
        String estado = approvalsWhatsapp.path("status").asText(null);
        if (!tieneTexto(estado)) {
            estado = primerTexto(approvalsWhatsapp);
        }
        if (!tieneTexto(categoria)) {
            categoria = primerTexto(approvalsWhatsapp.path("category"));
        }
        String tipoPlantilla = resolverTipoPlantilla(contenido.path("types"));
        return new PlantillaTwilioWhatsapp(sid, nombre, idioma, categoria, estado, tipoPlantilla);
    }

    private String resolverTipoPlantilla(JsonNode types) {
        if (!types.isObject()) {
            return "CONTENT_TEMPLATE";
        }
        Iterator<String> nombres = types.fieldNames();
        if (nombres.hasNext()) {
            return nombres.next();
        }
        return "CONTENT_TEMPLATE";
    }

    private String primerTexto(JsonNode nodo) {
        if (nodo == null || nodo.isMissingNode() || nodo.isNull()) {
            return null;
        }
        if (nodo.isTextual()) {
            return nodo.asText();
        }
        if (nodo.isObject()) {
            Iterator<JsonNode> valores = nodo.elements();
            while (valores.hasNext()) {
                String valor = primerTexto(valores.next());
                if (tieneTexto(valor)) {
                    return valor;
                }
            }
        }
        if (nodo.isArray()) {
            for (JsonNode item : nodo) {
                String valor = primerTexto(item);
                if (tieneTexto(valor)) {
                    return valor;
                }
            }
        }
        return null;
    }

    private String extraerSiguienteUrl(JsonNode nextPageUrl) {
        return extraerSiguienteUrlBase(nextPageUrl, "https://content.twilio.com");
    }

    private String extraerSiguienteUrlBase(JsonNode nextPageUrl, String baseUrl) {
        if (!nextPageUrl.isTextual() || nextPageUrl.asText().isBlank()) {
            return null;
        }
        return nextPageUrl.asText().replace(baseUrl, "");
    }

    private record RespuestaTwilio(
            String sid,
            String status,
            String errorCode,
            String errorMessage
    ) {
    }

    private record RespuestaSubcuentaTwilio(
            String sid,
            String authToken,
            String friendlyName,
            String ownerAccountSid,
            String status
    ) {
    }

    private record RespuestaMessagingServiceTwilio(
            String sid,
            String accountSid,
            String friendlyName,
            String inboundRequestUrl,
            String statusCallback
    ) {
    }

    public record PlantillaTwilioWhatsapp(
            String sid,
            String nombre,
            String idioma,
            String categoria,
            String estado,
            String tipoPlantilla
    ) {
    }

    public record SubcuentaTwilioProvisionada(
            String sid,
            String authToken,
            String friendlyName,
            String ownerAccountSid,
            String status
    ) {
    }

    public record MessagingServiceProvisionado(
            String sid,
            String friendlyName,
            String inboundRequestUrl,
            String statusCallbackUrl,
            String accountSid
    ) {
    }

    public record SenderTwilioWhatsapp(
            String sid,
            String senderId,
            String status,
            String displayName,
            String wabaId
    ) {
    }
}
