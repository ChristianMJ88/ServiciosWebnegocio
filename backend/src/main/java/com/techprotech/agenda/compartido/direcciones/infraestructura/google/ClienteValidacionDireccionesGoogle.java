package com.techprotech.agenda.compartido.direcciones.infraestructura.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.techprotech.agenda.compartido.direcciones.dominio.Direccion;
import com.techprotech.agenda.compartido.direcciones.dominio.ResultadoValidacionDireccion;
import com.techprotech.agenda.compartido.direcciones.dominio.ValidadorDireccionPort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class ClienteValidacionDireccionesGoogle implements ValidadorDireccionPort {
    private final RestClient cliente;
    private final PropiedadesGoogleMaps propiedades;

    public ClienteValidacionDireccionesGoogle(RestClient.Builder builder, PropiedadesGoogleMaps propiedades) {
        this.propiedades = propiedades;
        this.cliente = builder.baseUrl(propiedades.addressValidationBaseUrl()).build();
    }

    @Override
    public ResultadoValidacionDireccion validar(Direccion direccion) {
        if (!propiedades.servidorDisponible()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "La validación de direcciones todavía no está configurada");
        }
        try {
            JsonNode respuesta = cliente.post()
                    .uri("/v1:validateAddress")
                    .header("X-Goog-Api-Key", propiedades.serverApiKey())
                    .header("X-Goog-FieldMask", "result.verdict,result.address,result.geocode.location,result.geocode.placeId")
                    .body(cuerpo(direccion))
                    .retrieve()
                    .body(JsonNode.class);
            return mapear(respuesta, direccion);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Google Maps no pudo validar la dirección en este momento");
        }
    }

    private Object cuerpo(Direccion direccion) {
        List<String> lineas = new ArrayList<>();
        lineas.add(String.join(" ", noVacios(direccion.calle(), direccion.numeroExterior(), direccion.numeroInterior())));
        lineas.add(direccion.colonia());
        lineas.add(String.join(", ", noVacios(direccion.municipio(), direccion.estado(), direccion.codigoPostal())));
        return new Solicitud(new DireccionPostal(direccion.codigoPais().toUpperCase(Locale.ROOT), lineas));
    }

    private ResultadoValidacionDireccion mapear(JsonNode respuesta, Direccion original) {
        JsonNode resultado = respuesta == null ? null : respuesta.path("result");
        if (resultado == null || resultado.isMissingNode() || resultado.isNull()) {
            return invalida("NO_ENCONTRADA", "No encontramos una dirección verificable con esos datos", original);
        }
        JsonNode veredicto = resultado.path("verdict");
        String granularidad = veredicto.path("validationGranularity").asText("");
        boolean completa = veredicto.path("addressComplete").asBoolean(false);
        boolean sinConfirmar = veredicto.path("hasUnconfirmedComponents").asBoolean(false);
        boolean precisa = "PREMISE".equals(granularidad) || "SUB_PREMISE".equals(granularidad);
        JsonNode direccionGoogle = resultado.path("address");
        JsonNode postal = direccionGoogle.path("postalAddress");
        Direccion normalizada = normalizada(direccionGoogle.path("addressComponents"), postal, original);
        boolean valida = completa && !sinConfirmar && precisa;
        String estado = valida ? "VALIDADA" : completa ? "REQUIERE_CONFIRMACION" : "INCOMPLETA";
        String mensaje = valida ? "Dirección verificada por Google Maps"
                : precisa ? "Google Maps necesita que confirmes algunos datos de la dirección"
                : "Agrega una calle y número que Google Maps pueda ubicar con precisión";
        JsonNode geocodigo = resultado.path("geocode");
        return new ResultadoValidacionDireccion(valida, estado, mensaje,
                texto(direccionGoogle, "formattedAddress"), normalizada, texto(geocodigo, "placeId"),
                decimal(geocodigo.path("location"), "latitude"), decimal(geocodigo.path("location"), "longitude"));
    }

    private Direccion normalizada(JsonNode componentes, JsonNode postal, Direccion original) {
        return new Direccion(
                primero(componente(componentes, "route"), original.calle()),
                primero(componente(componentes, "street_number"), original.numeroExterior()),
                primero(componente(componentes, "subpremise"), original.numeroInterior()),
                primero(componente(componentes, "neighborhood"), componente(componentes, "sublocality_level_1"),
                        texto(postal, "sublocality"), original.colonia()),
                primero(componente(componentes, "locality"), componente(componentes, "administrative_area_level_2"),
                        texto(postal, "locality"), original.municipio()),
                primero(componente(componentes, "administrative_area_level_1"), texto(postal, "administrativeArea"), original.estado()),
                primero(componente(componentes, "postal_code"), texto(postal, "postalCode"), original.codigoPostal()),
                primero(texto(postal, "regionCode"), original.codigoPais()).toUpperCase(Locale.ROOT)
        );
    }

    private String componente(JsonNode componentes, String tipo) {
        if (!componentes.isArray()) return null;
        for (JsonNode componente : componentes) {
            if (tipo.equals(componente.path("componentType").asText())) {
                return primero(texto(componente.path("componentName"), "text"), texto(componente, "componentName"));
            }
        }
        return null;
    }

    private ResultadoValidacionDireccion invalida(String estado, String mensaje, Direccion original) {
        return new ResultadoValidacionDireccion(false, estado, mensaje, null, original, null, null, null);
    }

    private List<String> noVacios(String... valores) {
        List<String> resultado = new ArrayList<>();
        for (String valor : valores) if (valor != null && !valor.isBlank()) resultado.add(valor.trim());
        return resultado;
    }

    private String texto(JsonNode nodo, String campo) {
        String valor = nodo.path(campo).asText(null);
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private BigDecimal decimal(JsonNode nodo, String campo) {
        return nodo.path(campo).isNumber() ? nodo.path(campo).decimalValue() : null;
    }

    private String primero(String... valores) {
        for (String valor : valores) if (valor != null && !valor.isBlank()) return valor.trim();
        return null;
    }

    private record Solicitud(DireccionPostal address) {}
    private record DireccionPostal(String regionCode, List<String> addressLines) {}
}
