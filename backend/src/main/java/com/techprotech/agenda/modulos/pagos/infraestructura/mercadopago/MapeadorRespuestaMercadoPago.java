package com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago;

import com.fasterxml.jackson.databind.JsonNode;
import com.techprotech.agenda.modulos.pagos.dominio.DesgloseLiquidacionPago;
import com.techprotech.agenda.modulos.pagos.dominio.EstadoPago;
import com.techprotech.agenda.modulos.pagos.dominio.ProveedorPago;
import com.techprotech.agenda.modulos.pagos.dominio.ResultadoCobro;
import com.techprotech.agenda.modulos.pagos.dominio.ResultadoReembolso;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Set;

@Component
class MapeadorRespuestaMercadoPago {
    ResultadoCobro cobro(JsonNode respuesta) {
        String estado = texto(respuesta, "status");
        String detalle = texto(respuesta, "status_detail");
        EstadoPago estadoPago = estado(estado, detalle);
        return new ResultadoCobro(ProveedorPago.MERCADO_PAGO, texto(respuesta, "id"), referenciaPago(respuesta), estadoPago,
                accion(respuesta), detalle == null ? estado : detalle, detalle,
                fecha(respuesta.path("last_updated_date")), liquidacion(respuesta, estadoPago));
    }

    DesgloseLiquidacionPago liquidacion(JsonNode respuesta, EstadoPago estado) {
        JsonNode pago = respuesta.path("transactions").path("payments").path(0);
        BigDecimal bruto = primerDecimal(respuesta.path("total_amount"), pago.path("amount"), pago.path("transaction_amount"));
        BigDecimal comisionProveedor = sumarComisiones(pago.path("fee_details"),
                Set.of("mercadopago_fee", "payment_fee", "financing_fee"));
        BigDecimal comisionPlataforma = primerDecimal(pago.path("marketplace_fee"), respuesta.path("marketplace_fee"));
        BigDecimal comisionPlataformaDetalle = sumarComisiones(pago.path("fee_details"),
                Set.of("marketplace_fee", "application_fee"));
        if (comisionPlataforma == null) comisionPlataforma = comisionPlataformaDetalle;
        BigDecimal neto = primerDecimal(pago.path("transaction_details").path("net_received_amount"),
                pago.path("net_received_amount"), respuesta.path("settlement_net_amount"));
        BigDecimal reembolsado = sumarImportes(respuesta.path("transactions").path("refunds"));
        String moneda = valorO(texto(respuesta, "currency"), texto(respuesta, "currency_id"));
        if (moneda == null) moneda = valorO(texto(pago, "currency"), texto(pago, "currency_id"));
        boolean hayDetalle = bruto != null || comisionProveedor != null || comisionPlataforma != null
                || neto != null || reembolsado != null;
        if (!hayDetalle) return null;
        // La respuesta de la orden es operativa. La liquidación definitiva sólo proviene
        // de los Reportes de Liberaciones/Settlement descargados desde la API de Reportes.
        boolean definitiva = false;
        return new DesgloseLiquidacionPago(moneda, bruto, comisionProveedor, null, comisionPlataforma,
                null, null, reembolsado, neto, definitiva,
                null);
    }

    ResultadoReembolso reembolso(JsonNode respuesta, BigDecimal importeSolicitado) {
        JsonNode reembolso = respuesta.path("transactions").path("refunds").path(0);
        return new ResultadoReembolso(
                texto(reembolso, "id") == null ? texto(respuesta, "id") : texto(reembolso, "id"),
                estado(texto(respuesta, "status"), texto(respuesta, "status_detail")),
                decimal(reembolso.path("amount"), importeSolicitado),
                fecha(respuesta.path("last_updated_date")));
    }

    EstadoPago estado(String estado, String detalle) {
        String valor = normalizar(estado);
        String especifico = normalizar(detalle);
        if (contiene(especifico, "refund")) return EstadoPago.REEMBOLSADO;
        if (contiene(especifico, "reject", "denied", "failed", "error")) return EstadoPago.RECHAZADO;
        if (contiene(especifico, "cancel")) return EstadoPago.CANCELADO;
        if (contiene(especifico, "accredited", "approved")) return EstadoPago.APROBADO;
        return switch (valor) {
            case "created" -> EstadoPago.CREADO;
            case "action_required" -> EstadoPago.REQUIERE_ACCION;
            case "processing", "in_process", "pending" -> EstadoPago.PROCESANDO;
            case "processed", "approved" -> EstadoPago.APROBADO;
            case "rejected", "failed" -> EstadoPago.RECHAZADO;
            case "cancelled", "canceled" -> EstadoPago.CANCELADO;
            case "refunded" -> EstadoPago.REEMBOLSADO;
            default -> EstadoPago.DESCONOCIDO;
        };
    }

    private String accion(JsonNode respuesta) {
        JsonNode metodo = respuesta.path("transactions").path("payments").path(0).path("payment_method");
        String seguridad = texto(metodo.path("transaction_security"), "url");
        if (seguridad != null) return seguridad;
        String ticket = texto(metodo, "ticket_url");
        return ticket == null ? texto(respuesta, "checkout_url") : ticket;
    }

    private String referenciaPago(JsonNode respuesta) {
        return texto(respuesta.path("transactions").path("payments").path(0), "id");
    }

    private OffsetDateTime fecha(JsonNode nodo) {
        try {
            if (!nodo.isMissingNode() && !nodo.isNull()) return OffsetDateTime.parse(nodo.asText());
        } catch (Exception ignored) {
            // Una fecha inválida del proveedor no debe ocultar el estado del pago.
        }
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private BigDecimal decimal(JsonNode nodo, BigDecimal predeterminado) {
        try {
            return nodo.isMissingNode() || nodo.isNull() ? predeterminado : new BigDecimal(nodo.asText());
        } catch (NumberFormatException ignored) {
            return predeterminado;
        }
    }

    private BigDecimal primerDecimal(JsonNode... nodos) {
        for (JsonNode nodo : nodos) {
            BigDecimal valor = decimal(nodo, null);
            if (valor != null) return valor;
        }
        return null;
    }

    private BigDecimal sumarComisiones(JsonNode detalles, Set<String> tipos) {
        if (!detalles.isArray()) return null;
        BigDecimal total = BigDecimal.ZERO;
        boolean encontrado = false;
        for (JsonNode detalle : detalles) {
            String tipo = normalizar(texto(detalle, "type"));
            if (!tipos.contains(tipo)) continue;
            BigDecimal importe = decimal(detalle.path("amount"), null);
            if (importe != null) {
                total = total.add(importe.abs());
                encontrado = true;
            }
        }
        return encontrado ? total : null;
    }

    private BigDecimal sumarImportes(JsonNode items) {
        if (!items.isArray() || items.isEmpty()) return null;
        BigDecimal total = BigDecimal.ZERO;
        boolean encontrado = false;
        for (JsonNode item : items) {
            BigDecimal importe = decimal(item.path("amount"), null);
            if (importe != null) {
                total = total.add(importe.abs());
                encontrado = true;
            }
        }
        return encontrado ? total : null;
    }

    private String valorO(String primero, String segundo) { return primero == null ? segundo : primero; }

    private boolean contiene(String valor, String... fragmentos) {
        for (String fragmento : fragmentos) if (valor.contains(fragmento)) return true;
        return false;
    }

    private String normalizar(String valor) { return valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT); }

    private String texto(JsonNode nodo, String campo) {
        JsonNode valor = nodo.path(campo);
        return valor.isMissingNode() || valor.isNull() || valor.asText().isBlank() ? null : valor.asText();
    }
}
