package com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago;

import com.fasterxml.jackson.databind.JsonNode;
import com.techprotech.agenda.modulos.pagos.dominio.EstadoPago;
import com.techprotech.agenda.modulos.pagos.dominio.ProveedorPago;
import com.techprotech.agenda.modulos.pagos.dominio.ResultadoCobro;
import com.techprotech.agenda.modulos.pagos.dominio.ResultadoReembolso;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

@Component
class MapeadorRespuestaMercadoPago {
    ResultadoCobro cobro(JsonNode respuesta) {
        String estado = texto(respuesta, "status");
        String detalle = texto(respuesta, "status_detail");
        return new ResultadoCobro(ProveedorPago.MERCADO_PAGO, texto(respuesta, "id"), estado(estado, detalle),
                accion(respuesta), detalle == null ? estado : detalle, detalle,
                fecha(respuesta.path("last_updated_date")));
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
