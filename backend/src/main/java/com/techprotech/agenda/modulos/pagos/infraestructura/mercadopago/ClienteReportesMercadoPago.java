package com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago;

import com.fasterxml.jackson.databind.JsonNode;
import com.techprotech.agenda.modulos.pagos.aplicacion.ServicioOAuthMercadoPago;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.List;

@Component
public class ClienteReportesMercadoPago {
    private static final String BASE = "https://api.mercadopago.com/v1/account/release_report";
    private final ServicioOAuthMercadoPago oauth;
    private final RestClient cliente;

    public ClienteReportesMercadoPago(ServicioOAuthMercadoPago oauth, RestClient.Builder builder) {
        this.oauth = oauth;
        this.cliente = builder.build();
    }

    public TareaReporte crear(Long empresaId, OffsetDateTime desde, OffsetDateTime hasta) {
        JsonNode respuesta = cliente.post().uri(BASE).contentType(MediaType.APPLICATION_JSON)
                .headers(h -> h.setBearerAuth(oauth.obtenerAccessToken(empresaId)))
                .body(Map.of("begin_date", utc(desde), "end_date", utc(hasta)))
                .retrieve().body(JsonNode.class);
        return mapear(respuesta);
    }

    public ConfiguracionReporte consultarConfiguracion(Long empresaId) {
        JsonNode respuesta = cliente.get().uri(BASE + "/config")
                .headers(h -> h.setBearerAuth(oauth.obtenerAccessToken(empresaId)))
                .retrieve().body(JsonNode.class);
        if (respuesta == null) throw new IllegalStateException("Mercado Pago no devolvió la configuración de reportes");
        List<String> columnas = new java.util.ArrayList<>();
        respuesta.path("columns").forEach(item -> { String key=texto(item,"key"); if(key!=null) columnas.add(key); });
        JsonNode frecuencia=respuesta.path("frequency");
        return new ConfiguracionReporte(texto(respuesta,"file_name_prefix"), texto(respuesta,"display_timezone"),
                respuesta.path("scheduled").asBoolean(false), texto(respuesta,"report_translation"), columnas,
                texto(frecuencia,"type"), frecuencia.path("hour").isNumber()?frecuencia.path("hour").asInt():null,
                frecuencia.path("value").isMissingNode()?null:frecuencia.path("value").asText());
    }

    public TareaReporte consultar(Long empresaId, String tareaId) {
        JsonNode respuesta = cliente.get().uri(BASE + "/task/" + segmento(tareaId))
                .headers(h -> h.setBearerAuth(oauth.obtenerAccessToken(empresaId)))
                .retrieve().body(JsonNode.class);
        return mapear(respuesta);
    }

    public byte[] descargar(Long empresaId, String archivo) {
        byte[] contenido = cliente.get().uri(BASE + "/" + UriUtils.encodePathSegment(archivo, StandardCharsets.UTF_8))
                .headers(h -> h.setBearerAuth(oauth.obtenerAccessToken(empresaId)))
                .retrieve().body(byte[].class);
        if (contenido == null || contenido.length == 0) throw new IllegalStateException("Mercado Pago devolvió un reporte vacío");
        return contenido;
    }

    private TareaReporte mapear(JsonNode nodo) {
        if (nodo == null) throw new IllegalStateException("Mercado Pago no devolvió información del reporte");
        return new TareaReporte(texto(nodo, "id"), texto(nodo, "report_id"), texto(nodo, "status"),
                texto(nodo, "file_name"));
    }
    private String utc(OffsetDateTime fecha) { return fecha.withOffsetSameInstant(java.time.ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME); }
    private String segmento(String value) { if (value == null || !value.matches("[A-Za-z0-9_-]+")) throw new IllegalArgumentException("Tarea de reporte inválida"); return value; }
    private String texto(JsonNode nodo, String campo) { JsonNode valor=nodo.path(campo); return valor.isMissingNode()||valor.isNull()||valor.asText().isBlank()?null:valor.asText(); }
    public record TareaReporte(String tareaId, String reporteId, String estado, String archivo) {}
    public record ConfiguracionReporte(String prefijo, String zonaHoraria, boolean programado, String idioma,
                                       List<String> columnas, String frecuenciaTipo, Integer frecuenciaHora,
                                       String frecuenciaValor) {}
}
