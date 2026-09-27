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
            if (respuesta == null || !respuesta.path("success").asBoolean(false)) {
                return new GeocodigoResponse(false, codigoPostal, null, null, pais, null, null);
            }
            JsonNode data = respuesta.path("data");
            JsonNode coordenadas = data.path("coordinates");
            return new GeocodigoResponse(true, texto(data, "zipcode"), texto(data, "city"), texto(data, "state"),
                    texto(data, "country"), texto(coordenadas, "latitude"), texto(coordenadas, "longitude"));
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No fue posible validar el código postal con Envia.com");
        }
    }

    private String texto(JsonNode nodo, String campo) {
        String valor = nodo.path(campo).asText(null);
        return valor == null || valor.isBlank() ? null : valor;
    }
}
