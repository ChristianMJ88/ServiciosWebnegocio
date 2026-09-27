package com.techprotech.agenda.modulos.envios.infraestructura.envia;

import com.fasterxml.jackson.databind.JsonNode;
import com.techprotech.agenda.modulos.envios.api.dto.GeocodigoResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ClienteGeocodigosEnvia {
    private final RestClient cliente;

    public ClienteGeocodigosEnvia(RestClient.Builder builder, PropiedadesEnvia propiedades) {
        this.cliente = builder.baseUrl(propiedades.geocodigosBaseUrl()).build();
    }

    public GeocodigoResponse validar(String pais, String codigoPostal) {
        try {
            JsonNode respuesta = cliente.get().uri("/zipcode/{pais}/{codigoPostal}", pais, codigoPostal).retrieve().body(JsonNode.class);
            if (respuesta == null || respuesta.isNull() || (respuesta.isArray() && respuesta.isEmpty())) {
                return new GeocodigoResponse(false, codigoPostal, null, null, pais, null, null);
            }
            JsonNode data = respuesta.isArray() ? respuesta.path(0) : respuesta.path("data");
            if (!respuesta.isArray() && !respuesta.path("success").asBoolean(false)) {
                return new GeocodigoResponse(false, codigoPostal, null, null, pais, null, null);
            }
            JsonNode coordenadas = data.path("coordinates");
            String codigo = primero(texto(data, "zipcode"), texto(data, "zip_code"), codigoPostal);
            String municipio = primero(texto(data, "city"), texto(data, "locality"), texto(data.path("regions"), "region_2"));
            String estado = data.path("state").isObject() ? texto(data.path("state"), "name") : texto(data, "state");
            String codigoPais = data.path("country").isObject() ? texto(data.path("country"), "code") : texto(data, "country");
            return new GeocodigoResponse(true, codigo, municipio, estado, primero(codigoPais, pais),
                    texto(coordenadas, "latitude"), texto(coordenadas, "longitude"));
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No fue posible validar el código postal con Envia.com");
        }
    }

    private String texto(JsonNode nodo, String campo) {
        String valor = nodo.path(campo).asText(null);
        return valor == null || valor.isBlank() ? null : valor;
    }

    private String primero(String... valores) {
        for (String valor : valores) if (valor != null && !valor.isBlank()) return valor;
        return null;
    }
}
