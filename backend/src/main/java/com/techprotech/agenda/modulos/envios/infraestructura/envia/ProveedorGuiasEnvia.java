package com.techprotech.agenda.modulos.envios.infraestructura.envia;

import com.fasterxml.jackson.databind.JsonNode;
import com.techprotech.agenda.modulos.envios.dominio.ProveedorGuiasEnvio;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ProveedorGuiasEnvia implements ProveedorGuiasEnvio {
    private final RestClient.Builder builder;
    private final PropiedadesEnvia propiedades;

    public ProveedorGuiasEnvia(RestClient.Builder builder, PropiedadesEnvia propiedades) {
        this.builder = builder;
        this.propiedades = propiedades;
    }

    @Override
    public String codigo() { return "ENVIA"; }

    @Override
    public Guia generar(String token, String ambiente, Solicitud solicitud) {
        try {
            JsonNode respuesta = cliente(ambiente).post().uri("/ship/generate/")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .body(cuerpo(solicitud)).retrieve().body(JsonNode.class);
            JsonNode item = primero(respuesta);
            String numeroGuia = texto(item, "trackingNumber", "tracking_number", "tracking");
            String etiqueta = texto(item, "label", "labelUrl", "label_url");
            if (numeroGuia == null || etiqueta == null) {
                throw new IllegalStateException("Envia.com no devolvió la guía y la etiqueta esperadas");
            }
            return new Guia(texto(item, "shipmentId", "shipment_id", "id"), numeroGuia, etiqueta,
                    texto(item, "trackUrl", "trackingUrl", "track_url"),
                    decimal(item, "totalPrice", "total_price", "price"), texto(item, "status"));
        } catch (RestClientException ex) {
            throw new IllegalStateException("Envia.com no pudo generar la guía", ex);
        }
    }

    @Override
    public Rastreo rastrear(String token, String ambiente, String numeroGuia) {
        try {
            JsonNode respuesta = cliente(ambiente).post().uri("/ship/generaltrack/")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .body(Map.of("trackingNumbers", new String[]{numeroGuia}))
                    .retrieve().body(JsonNode.class);
            JsonNode item = primero(respuesta);
            String estado = texto(item, "status", "statusName", "status_name");
            String descripcion = texto(item, "description", "statusDescription", "status_description");
            String referencia = texto(item, "eventId", "event_id");
            if (referencia == null && estado != null) referencia = numeroGuia + ":" + estado + ":" + descripcion;
            return new Rastreo(estado, descripcion, referencia);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Envia.com no pudo consultar el rastreo", ex);
        }
    }

    private RestClient cliente(String ambiente) {
        String baseUrl = "PRODUCCION".equals(ambiente)
                ? propiedades.apiProduccionBaseUrl() : propiedades.apiPruebasBaseUrl();
        return builder.baseUrl(baseUrl).build();
    }

    private Map<String, Object> cuerpo(Solicitud solicitud) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("origin", direccion(solicitud.origen(), "origin"));
        cuerpo.put("destination", direccion(solicitud.destino(), "destination"));
        cuerpo.put("packages", solicitud.paquetes().stream().map(this::paquete).toList());
        cuerpo.put("settings", Map.of("printFormat", "PDF", "printSize", "PAPER_4X6",
                "currency", solicitud.moneda()));
        cuerpo.put("shipment", Map.of("type", 1, "carrier", solicitud.paqueteria(),
                "service", solicitud.servicio()));
        return cuerpo;
    }

    private Map<String, Object> direccion(Direccion direccion, String tipo) {
        Map<String, Object> salida = new LinkedHashMap<>();
        poner(salida, "type", tipo); poner(salida, "name", direccion.nombre());
        poner(salida, "company", direccion.empresa()); poner(salida, "email", direccion.correo());
        poner(salida, "phone", direccion.telefono()); poner(salida, "street", direccion.calle());
        poner(salida, "number", direccion.numero()); poner(salida, "district", direccion.colonia());
        poner(salida, "city", direccion.municipio()); poner(salida, "state", direccion.estado());
        poner(salida, "country", direccion.pais()); poner(salida, "postalCode", direccion.codigoPostal());
        poner(salida, "reference", direccion.referencias());
        return salida;
    }

    private Map<String, Object> paquete(Paquete paquete) {
        return Map.of("type", "box", "content", paquete.contenido(), "amount", paquete.cantidad(),
                "declaredValue", paquete.valorDeclarado(), "weight", paquete.pesoKg(), "weightUnit", "KG",
                "lengthUnit", "CM", "dimensions", Map.of("length", paquete.largoCm(),
                        "width", paquete.anchoCm(), "height", paquete.altoCm()));
    }

    private JsonNode primero(JsonNode respuesta) {
        if (respuesta == null) throw new IllegalStateException("Envia.com devolvió una respuesta vacía");
        JsonNode datos = respuesta.isArray() ? respuesta : respuesta.path("data");
        if (datos.isArray() && !datos.isEmpty()) return datos.get(0);
        if (datos.isObject()) return datos;
        throw new IllegalStateException("Envia.com devolvió una respuesta inesperada");
    }

    private String texto(JsonNode item, String... campos) {
        for (String campo : campos) {
            String valor = item.path(campo).asText(null);
            if (valor != null && !valor.isBlank()) return valor.trim();
        }
        return null;
    }

    private BigDecimal decimal(JsonNode item, String... campos) {
        String valor = texto(item, campos);
        if (valor == null) return null;
        try { return new BigDecimal(valor).setScale(2, RoundingMode.HALF_UP); }
        catch (NumberFormatException ex) { return null; }
    }

    private void poner(Map<String, Object> destino, String clave, String valor) {
        if (valor != null && !valor.isBlank()) destino.put(clave, valor.trim());
    }
}
