package com.techprotech.agenda.modulos.envios.infraestructura.envia;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ClienteConsultasEnvia {
    private final RestClient.Builder builder;
    private final PropiedadesEnvia propiedades;

    public ClienteConsultasEnvia(RestClient.Builder builder, PropiedadesEnvia propiedades) {
        this.builder = builder;
        this.propiedades = propiedades;
    }

    public int contarPaqueterias(String token, String ambiente, String codigoPais) {
        String baseUrl = "PRODUCCION".equals(ambiente)
                ? propiedades.consultasProduccionBaseUrl()
                : propiedades.consultasPruebasBaseUrl();
        try {
            JsonNode respuesta = builder.baseUrl(baseUrl).build().get()
                    .uri("/available-carrier/{pais}/0/1", codigoPais)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode datos = respuesta == null ? null : (respuesta.isArray() ? respuesta : respuesta.path("data"));
            return datos != null && datos.isArray() ? datos.size() : 0;
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La credencial de Envia.com no es válida para el ambiente seleccionado");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No fue posible comprobar la conexión con Envia.com");
        }
    }
}
