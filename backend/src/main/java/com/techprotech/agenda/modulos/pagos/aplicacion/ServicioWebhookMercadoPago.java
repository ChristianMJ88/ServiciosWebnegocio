package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.EventoWebhookPagoEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.ProveedorPagoEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago.PropiedadesMercadoPago;
import com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago.ValidadorFirmaWebhookMercadoPago;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.EmpresaProveedorPagoRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.EventoWebhookPagoRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.ProveedorPagoRepositorio;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServicioWebhookMercadoPago {
    private static final String CODIGO_PROVEEDOR = "MERCADO_PAGO";

    private final PropiedadesMercadoPago propiedades;
    private final ValidadorFirmaWebhookMercadoPago validador;
    private final ProveedorPagoRepositorio proveedores;
    private final EmpresaProveedorPagoRepositorio conexiones;
    private final EventoWebhookPagoRepositorio eventos;
    private final ObjectMapper objectMapper;

    public ServicioWebhookMercadoPago(
            PropiedadesMercadoPago propiedades,
            ValidadorFirmaWebhookMercadoPago validador,
            ProveedorPagoRepositorio proveedores,
            EmpresaProveedorPagoRepositorio conexiones,
            EventoWebhookPagoRepositorio eventos,
            ObjectMapper objectMapper
    ) {
        this.propiedades = propiedades;
        this.validador = validador;
        this.proveedores = proveedores;
        this.conexiones = conexiones;
        this.eventos = eventos;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void recibir(String xSignature, String xRequestId, String dataId, String payload) {
        if (!propiedades.habilitado() || vacio(propiedades.webhookSecret())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Webhook de Mercado Pago no configurado");
        }
        if (!validador.validar(xSignature, xRequestId, dataId, propiedades.webhookSecret())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Firma de Mercado Pago inválida");
        }
        try {
            JsonNode cuerpo = objectMapper.readTree(payload);
            ProveedorPagoEntidad proveedor = proveedores.findByCodigoAndActivoTrue(CODIGO_PROVEEDOR)
                    .orElseThrow(() -> new IllegalStateException("Mercado Pago no está disponible"));
            String eventoId = texto(cuerpo, "id");
            if (vacio(eventoId)) eventoId = xRequestId;
            if (eventos.existsByProveedorPagoIdAndEventoExternoId(proveedor.getId(), eventoId)) return;

            EventoWebhookPagoEntidad evento = new EventoWebhookPagoEntidad();
            evento.setProveedorPagoId(proveedor.getId());
            String cuentaId = texto(cuerpo, "user_id");
            if (!vacio(cuentaId)) {
                conexiones.findByProveedorPagoIdAndCuentaExternaId(proveedor.getId(), cuentaId)
                        .ifPresent(conexion -> evento.setEmpresaId(conexion.getEmpresaId()));
            }
            evento.setEventoExternoId(eventoId);
            evento.setTipo(valorO(texto(cuerpo, "type"), "desconocido"));
            evento.setModoProduccion(cuerpo.path("live_mode").asBoolean(false));
            evento.setEstadoProcesamiento("RECIBIDO");
            evento.setIntentos(0);
            evento.setPayload(payload);
            eventos.saveAndFlush(evento);
        } catch (DataIntegrityViolationException ignored) {
            // Otro hilo persistió el mismo evento; la entrega ya quedó registrada.
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook de Mercado Pago inválido", ex);
        }
    }

    private String texto(JsonNode nodo, String campo) {
        JsonNode valor = nodo.path(campo);
        return valor.isMissingNode() || valor.isNull() ? null : valor.asText();
    }

    private String valorO(String valor, String predeterminado) {
        return vacio(valor) ? predeterminado : valor;
    }

    private boolean vacio(String valor) { return valor == null || valor.isBlank(); }
}
