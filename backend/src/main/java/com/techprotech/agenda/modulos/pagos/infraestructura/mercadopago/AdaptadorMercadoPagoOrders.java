package com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.pagos.aplicacion.ServicioOAuthMercadoPago;
import com.techprotech.agenda.modulos.pagos.dominio.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AdaptadorMercadoPagoOrders implements ProveedorPagosPort {
    private static final String ORDERS = "https://api.mercadopago.com/v1/orders";

    private final ServicioOAuthMercadoPago oauth;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final MapeadorRespuestaMercadoPago mapeador;
    private final ValidadorFirmaWebhookMercadoPago validadorFirma;
    private final PropiedadesMercadoPago propiedades;

    public AdaptadorMercadoPagoOrders(ServicioOAuthMercadoPago oauth, RestClient.Builder restClientBuilder,
                                      ObjectMapper objectMapper, MapeadorRespuestaMercadoPago mapeador,
                                      ValidadorFirmaWebhookMercadoPago validadorFirma,
                                      PropiedadesMercadoPago propiedades) {
        this.oauth = oauth;
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.mapeador = mapeador;
        this.validadorFirma = validadorFirma;
        this.propiedades = propiedades;
    }

    @Override public ProveedorPago proveedor() { return ProveedorPago.MERCADO_PAGO; }

    @Override
    public ResultadoCobro crearCobro(SolicitudCobro solicitud) {
        validarCobro(solicitud);
        return mapeador.cobro(post(ORDERS, solicitud.empresaId(), solicitud.idempotencia(), cuerpoCobro(solicitud)));
    }

    @Override
    public ResultadoCobro consultarCobro(Long empresaId, String referenciaExterna) {
        requerido(empresaId, "empresaId");
        requerido(referenciaExterna, "referenciaExterna");
        return mapeador.cobro(get(ORDERS + "/" + segmento(referenciaExterna), empresaId));
    }

    @Override
    public ResultadoReembolso reembolsar(SolicitudReembolso solicitud) {
        validarReembolso(solicitud);
        Map<String, Object> transaccion = new LinkedHashMap<>();
        transaccion.put("id", solicitud.referenciaPagoExterna());
        if (solicitud.importe() != null) transaccion.put("amount", importe(solicitud.importe()));
        JsonNode respuesta = post(ORDERS + "/" + segmento(solicitud.referenciaOrdenExterna()) + "/refund",
                solicitud.empresaId(), solicitud.idempotencia(), Map.of("transactions", List.of(transaccion)));
        return mapeador.reembolso(respuesta, solicitud.importe());
    }

    @Override
    public EventoPagoProveedor validarWebhook(WebhookProveedorPago webhook) {
        requerido(webhook, "webhook");
        String firma = encabezado(webhook.encabezados(), "x-signature");
        String requestId = encabezado(webhook.encabezados(), "x-request-id");
        String dataId = parametro(webhook.parametros(), "data.id");
        if (!validadorFirma.validar(firma, requestId, dataId, propiedades.webhookSecret())) {
            throw new IllegalArgumentException("Firma de Mercado Pago inválida");
        }
        try {
            JsonNode cuerpo = objectMapper.readTree(webhook.payload());
            String referencia = texto(cuerpo.path("data"), "id");
            if (referencia == null) referencia = dataId;
            return new EventoPagoProveedor(proveedor(), valorO(texto(cuerpo, "id"), requestId),
                    valorO(texto(cuerpo, "type"), "desconocido"), referencia,
                    mapeador.estado(texto(cuerpo, "status"), texto(cuerpo, "status_detail")),
                    cuerpo.path("live_mode").asBoolean(false));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Webhook de Mercado Pago inválido", ex);
        }
    }

    private Map<String, Object> cuerpoCobro(SolicitudCobro solicitud) {
        Map<String, String> metadatos = solicitud.metadatos() == null ? Map.of() : solicitud.metadatos();
        Map<String, Object> metodo = new LinkedHashMap<>();
        metodo.put("id", requerido(metadatos.get("payment_method_id"), "payment_method_id"));
        metodo.put("type", requerido(metadatos.get("payment_type_id"), "payment_type_id"));
        metodo.put("token", solicitud.tokenMedioPago());
        metodo.put("installments", enteroPositivo(metadatos.get("installments"), "installments"));
        opcional(metodo, "statement_descriptor", metadatos.get("statement_descriptor"));

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("type", "online");
        cuerpo.put("processing_mode", "automatic");
        cuerpo.put("capture_mode", "automatic");
        cuerpo.put("total_amount", importe(solicitud.importe()));
        cuerpo.put("external_reference", solicitud.referenciaOrden());
        cuerpo.put("payer", Map.of("email", solicitud.correoPagador()));
        cuerpo.put("transactions", Map.of("payments", List.of(Map.of(
                "amount", importe(solicitud.importe()), "payment_method", metodo))));
        if (solicitud.comisionPlataforma() != null && solicitud.comisionPlataforma().signum() > 0) {
            cuerpo.put("marketplace_fee", importe(solicitud.comisionPlataforma()));
        }
        opcional(cuerpo, "description", metadatos.get("description"));
        return cuerpo;
    }

    private JsonNode post(String uri, Long empresaId, String idempotencia, Object cuerpo) {
        String token = oauth.obtenerAccessToken(empresaId);
        try {
            return respuestaRequerida(restClient.post().uri(uri).contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> { h.setBearerAuth(token); h.set("X-Idempotency-Key", idempotencia); })
                    .body(cuerpo).retrieve().body(JsonNode.class));
        } catch (RestClientResponseException ex) {
            return respuestaErrorOExcepcion(ex);
        }
    }

    private JsonNode get(String uri, Long empresaId) {
        try {
            return respuestaRequerida(restClient.get().uri(uri)
                    .headers(h -> h.setBearerAuth(oauth.obtenerAccessToken(empresaId)))
                    .retrieve().body(JsonNode.class));
        } catch (RestClientResponseException ex) {
            return respuestaErrorOExcepcion(ex);
        }
    }

    private JsonNode respuestaErrorOExcepcion(RestClientResponseException ex) {
        try {
            JsonNode respuesta = objectMapper.readTree(ex.getResponseBodyAsString());
            if (respuesta.hasNonNull("id")) return respuesta;
            String codigo = valorO(texto(respuesta, "code"), texto(respuesta, "error"));
            throw new IllegalStateException("Mercado Pago rechazó la operación" + (codigo == null ? "" : ": " + codigo));
        } catch (IllegalStateException error) {
            throw error;
        } catch (Exception ignored) {
            throw new IllegalStateException("Mercado Pago rechazó la operación (HTTP " + ex.getStatusCode().value() + ")");
        }
    }

    private JsonNode respuestaRequerida(JsonNode respuesta) {
        if (respuesta == null || !respuesta.hasNonNull("id")) throw new IllegalStateException("Mercado Pago no devolvió una orden válida");
        return respuesta;
    }

    private void validarCobro(SolicitudCobro solicitud) {
        requerido(solicitud, "solicitud"); requerido(solicitud.empresaId(), "empresaId");
        requerido(solicitud.referenciaOrden(), "referenciaOrden");
        if (solicitud.referenciaOrden().length() > 64) throw new IllegalArgumentException("referenciaOrden excede 64 caracteres");
        validarIdempotencia(solicitud.idempotencia()); validarImporte(solicitud.importe(), "importe");
        requerido(solicitud.moneda(), "moneda"); requerido(solicitud.tokenMedioPago(), "tokenMedioPago");
        requerido(solicitud.correoPagador(), "correoPagador");
        if (solicitud.comisionPlataforma() != null) {
            if (solicitud.comisionPlataforma().signum() < 0) throw new IllegalArgumentException("comisionPlataforma no puede ser negativa");
            if (solicitud.comisionPlataforma().compareTo(solicitud.importe()) >= 0)
                throw new IllegalArgumentException("comisionPlataforma debe ser menor que importe");
        }
    }

    private void validarReembolso(SolicitudReembolso solicitud) {
        requerido(solicitud, "solicitud"); requerido(solicitud.empresaId(), "empresaId");
        requerido(solicitud.referenciaOrdenExterna(), "referenciaOrdenExterna");
        requerido(solicitud.referenciaPagoExterna(), "referenciaPagoExterna");
        validarIdempotencia(solicitud.idempotencia());
        if (solicitud.importe() != null) validarImporte(solicitud.importe(), "importe");
    }

    private void validarIdempotencia(String valor) {
        requerido(valor, "idempotencia");
        if (valor.length() > 128) throw new IllegalArgumentException("idempotencia excede 128 caracteres");
    }
    private void validarImporte(BigDecimal valor, String campo) {
        if (valor == null || valor.signum() <= 0) throw new IllegalArgumentException(campo + " debe ser mayor que cero");
    }
    private String importe(BigDecimal valor) { return valor.setScale(2, RoundingMode.HALF_UP).toPlainString(); }
    private int enteroPositivo(String valor, String campo) {
        try {
            int numero = Integer.parseInt(requerido(valor, campo));
            if (numero < 1) throw new NumberFormatException();
            return numero;
        } catch (NumberFormatException ex) { throw new IllegalArgumentException(campo + " debe ser un entero positivo"); }
    }
    private String segmento(String valor) {
        if (!valor.matches("[A-Za-z0-9_-]+")) throw new IllegalArgumentException("Referencia externa inválida");
        return valor;
    }
    private String encabezado(Map<String, String> valores, String nombre) {
        if (valores == null) return null;
        return valores.entrySet().stream().filter(e -> nombre.equalsIgnoreCase(e.getKey())).map(Map.Entry::getValue).findFirst().orElse(null);
    }
    private String parametro(Map<String, String> valores, String nombre) { return valores == null ? null : valores.get(nombre); }
    private void opcional(Map<String, Object> destino, String llave, String valor) {
        if (valor != null && !valor.isBlank()) destino.put(llave, valor.trim());
    }
    private <T> T requerido(T valor, String campo) {
        if (valor == null || valor instanceof String texto && texto.isBlank()) throw new IllegalArgumentException("Falta " + campo);
        return valor;
    }
    private String texto(JsonNode nodo, String campo) {
        JsonNode valor = nodo.path(campo);
        return valor.isMissingNode() || valor.isNull() || valor.asText().isBlank() ? null : valor.asText();
    }
    private String valorO(String valor, String predeterminado) { return valor == null || valor.isBlank() ? predeterminado : valor; }
}
