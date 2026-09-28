package com.techprotech.agenda.modulos.envios.infraestructura.envia;

import com.fasterxml.jackson.databind.JsonNode;
import com.techprotech.agenda.modulos.envios.dominio.ProveedorCotizacionesEnvio;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.StreamSupport;

@Component
public class ProveedorCotizacionesEnvia implements ProveedorCotizacionesEnvio {
    private final RestClient.Builder builder;
    private final PropiedadesEnvia propiedades;
    private final ClienteConsultasEnvia consultas;

    public ProveedorCotizacionesEnvia(RestClient.Builder builder, PropiedadesEnvia propiedades,
                                      ClienteConsultasEnvia consultas) {
        this.builder = builder;
        this.propiedades = propiedades;
        this.consultas = consultas;
    }

    @Override
    public List<Opcion> cotizar(String token, String ambiente, Solicitud solicitud) {
        List<String> paqueterias = consultas.listarPaqueterias(token, ambiente, solicitud.origen().pais());
        if (paqueterias.isEmpty()) return List.of();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Resultado> resultados = paqueterias.stream()
                    .map(paqueteria -> executor.submit(() -> cotizarPaqueteria(token, ambiente, solicitud, paqueteria)))
                    .map(tarea -> {
                        try { return tarea.get(); }
                        catch (Exception ex) { return new Resultado(List.of(), true, false); }
                    }).toList();
            if (resultados.stream().anyMatch(Resultado::credencialInvalida)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La credencial de Envia.com no corresponde al ambiente configurado");
            }
            List<Opcion> opciones = resultados.stream().flatMap(item -> item.opciones().stream())
                    .sorted(Comparator.comparing(Opcion::costo)).toList();
            if (opciones.isEmpty() && resultados.stream().allMatch(Resultado::errorTecnico)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Envia.com no respondió al solicitar las tarifas");
            }
            return opciones;
        }
    }

    private Resultado cotizarPaqueteria(String token, String ambiente, Solicitud solicitud, String paqueteria) {
        String baseUrl = "PRODUCCION".equals(ambiente)
                ? propiedades.apiProduccionBaseUrl() : propiedades.apiPruebasBaseUrl();
        try {
            JsonNode respuesta = builder.baseUrl(baseUrl).build().post().uri("/ship/rate/")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .body(cuerpo(solicitud, paqueteria)).retrieve().body(JsonNode.class);
            JsonNode datos = respuesta == null ? null : (respuesta.isArray() ? respuesta : respuesta.path("data"));
            if (datos == null || !datos.isArray()) return new Resultado(List.of(), false, false);
            List<Opcion> opciones = StreamSupport.stream(datos.spliterator(), false)
                    .map(this::opcion).filter(Objects::nonNull).toList();
            return new Resultado(opciones, false, false);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
            return new Resultado(List.of(), false, true);
        } catch (HttpClientErrorException.UnprocessableEntity | HttpClientErrorException.BadRequest ex) {
            return new Resultado(List.of(), false, false);
        } catch (RestClientException ex) {
            return new Resultado(List.of(), true, false);
        }
    }

    private Map<String, Object> cuerpo(Solicitud solicitud, String paqueteria) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("origin", direccion(solicitud.origen(), "origin"));
        cuerpo.put("destination", direccion(solicitud.destino(), "destination"));
        cuerpo.put("packages", solicitud.paquetes().stream().map(this::paquete).toList());
        cuerpo.put("settings", Map.of("currency", solicitud.moneda()));
        cuerpo.put("shipment", Map.of("type", 1, "carrier", paqueteria));
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
        return Map.of(
                "type", "box", "content", paquete.contenido(), "amount", paquete.cantidad(),
                "declaredValue", paquete.valorDeclarado(), "weight", paquete.pesoKg(),
                "weightUnit", "KG", "lengthUnit", "CM",
                "dimensions", Map.of("length", paquete.largoCm(), "width", paquete.anchoCm(), "height", paquete.altoCm())
        );
    }

    private Opcion opcion(JsonNode item) {
        String paqueteria = texto(item, "carrier");
        String servicio = texto(item, "service");
        BigDecimal precio = decimal(item, "totalPrice");
        String moneda = texto(item, "currency");
        if (paqueteria == null || servicio == null || precio == null || precio.signum() < 0 || moneda == null) return null;
        return new Opcion(paqueteria, texto(item, "carrierDescription"), servicio,
                texto(item, "serviceDescription"), texto(item, "deliveryEstimate"), precio, moneda);
    }

    private String texto(JsonNode item, String campo) {
        String valor = item.path(campo).asText(null);
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private BigDecimal decimal(JsonNode item, String campo) {
        try { return new BigDecimal(item.path(campo).asText()).setScale(2, java.math.RoundingMode.HALF_UP); }
        catch (RuntimeException ex) { return null; }
    }

    private void poner(Map<String, Object> destino, String clave, String valor) {
        if (valor != null && !valor.isBlank()) destino.put(clave, valor.trim());
    }

    private record Resultado(List<Opcion> opciones, boolean errorTecnico, boolean credencialInvalida) {}
}
