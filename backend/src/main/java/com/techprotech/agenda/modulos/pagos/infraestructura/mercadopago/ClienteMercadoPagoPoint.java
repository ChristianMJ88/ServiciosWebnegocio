package com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago;

import com.fasterxml.jackson.databind.JsonNode;
import com.techprotech.agenda.modulos.pagos.aplicacion.ServicioOAuthMercadoPago;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ClienteMercadoPagoPoint {
    private static final String ORDERS = "https://api.mercadopago.com/v1/orders";
    private static final String TERMINALS = "https://api.mercadopago.com/terminals/v1/list?limit=50&offset=0";

    private final ServicioOAuthMercadoPago oauth;
    private final RestClient restClient;

    public ClienteMercadoPagoPoint(ServicioOAuthMercadoPago oauth, RestClient.Builder builder) {
        this.oauth = oauth;
        this.restClient = builder.build();
    }

    public List<TerminalRemota> listarTerminales(Long empresaId) {
        JsonNode respuesta = get(TERMINALS, empresaId);
        JsonNode terminales = respuesta.path("data").path("terminals");
        if (!terminales.isArray()) return List.of();
        List<TerminalRemota> resultado = new ArrayList<>();
        terminales.forEach(item -> resultado.add(new TerminalRemota(texto(item, "id"), texto(item, "pos_id"),
                texto(item, "store_id"), texto(item, "external_pos_id"), texto(item, "operating_mode"))));
        return resultado;
    }

    public OrdenRemota crearOrden(Long empresaId, String idempotencia, String terminalId, String referencia,
                                   BigDecimal importe, String descripcion) {
        Map<String, Object> point = new LinkedHashMap<>();
        point.put("terminal_id", terminalId);
        point.put("print_on_terminal", "no_ticket");
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("type", "point");
        cuerpo.put("external_reference", referencia);
        cuerpo.put("expiration_time", "PT16M");
        cuerpo.put("transactions", Map.of("payments", List.of(Map.of("amount", dinero(importe)))));
        cuerpo.put("config", Map.of("point", point));
        cuerpo.put("description", descripcion);
        return mapear(post(ORDERS, empresaId, idempotencia, cuerpo));
    }

    public OrdenRemota consultar(Long empresaId, String ordenId) {
        return mapear(get(ORDERS + "/" + segmento(ordenId), empresaId));
    }

    public OrdenRemota cancelar(Long empresaId, String ordenId, String idempotencia) {
        return mapear(post(ORDERS + "/" + segmento(ordenId) + "/cancel", empresaId, idempotencia, Map.of()));
    }

    public OrdenRemota reembolsar(Long empresaId, String ordenId, String idempotencia) {
        return mapear(post(ORDERS + "/" + segmento(ordenId) + "/refund", empresaId, idempotencia, Map.of()));
    }

    private OrdenRemota mapear(JsonNode respuesta) {
        JsonNode pago = respuesta.path("transactions").path("payments").path(0);
        return new OrdenRemota(requerido(respuesta, "id"), texto(respuesta, "status"), texto(respuesta, "status_detail"),
                texto(pago, "id"), texto(pago, "status"), texto(pago, "status_detail"));
    }

    private JsonNode get(String url, Long empresaId) {
        try {
            JsonNode respuesta = restClient.get().uri(url).headers(h -> h.setBearerAuth(oauth.obtenerAccessToken(empresaId)))
                    .retrieve().body(JsonNode.class);
            if (respuesta == null) throw new IllegalStateException("Mercado Pago no devolvió una respuesta");
            return respuesta;
        } catch (RestClientResponseException ex) { throw error(ex); }
    }

    private JsonNode post(String url, Long empresaId, String idempotencia, Object cuerpo) {
        try {
            JsonNode respuesta = restClient.post().uri(url).contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> { h.setBearerAuth(oauth.obtenerAccessToken(empresaId)); h.set("X-Idempotency-Key", idempotencia); })
                    .body(cuerpo).retrieve().body(JsonNode.class);
            if (respuesta == null) throw new IllegalStateException("Mercado Pago no devolvió una respuesta");
            return respuesta;
        } catch (RestClientResponseException ex) { throw error(ex); }
    }

    private IllegalStateException error(RestClientResponseException ex) {
        return new IllegalStateException("Mercado Pago rechazó la operación Point (HTTP " + ex.getStatusCode().value() + ")");
    }
    private String requerido(JsonNode nodo, String campo) { String valor = texto(nodo, campo); if (valor == null) throw new IllegalStateException("Mercado Pago no devolvió " + campo); return valor; }
    private String texto(JsonNode nodo, String campo) { JsonNode valor = nodo.path(campo); return valor.isMissingNode() || valor.isNull() || valor.asText().isBlank() ? null : valor.asText(); }
    private String segmento(String valor) { if (valor == null || !valor.matches("[A-Za-z0-9_-]+")) throw new IllegalArgumentException("Identificador de orden inválido"); return valor; }
    private String dinero(BigDecimal valor) { return valor.setScale(2, RoundingMode.HALF_UP).toPlainString(); }

    public record TerminalRemota(String terminalId, String posId, String storeId, String externalPosId, String modoOperacion) {}
    public record OrdenRemota(String ordenId, String estado, String detalleEstado, String pagoId, String estadoPago, String detallePago) {}
}
