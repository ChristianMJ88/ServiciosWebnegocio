package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.pagos.dominio.ProveedorPago;
import com.techprotech.agenda.modulos.pagos.dominio.ProveedorPagosPort;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.EventoWebhookPagoEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.IntentoPagoCheckoutEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.ProveedorPagoEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.EventoWebhookPagoRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.IntentoPagoCheckoutRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.ProveedorPagoRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class ProcesadorEventosPagoCheckout {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcesadorEventosPagoCheckout.class);

    private final EventoWebhookPagoRepositorio eventos;
    private final IntentoPagoCheckoutRepositorio intentos;
    private final ProveedorPagoRepositorio proveedores;
    private final List<ProveedorPagosPort> adaptadores;
    private final ServicioEstadoPagoCheckout estados;
    private final ObjectMapper objectMapper;

    public ProcesadorEventosPagoCheckout(EventoWebhookPagoRepositorio eventos,
                                         IntentoPagoCheckoutRepositorio intentos,
                                         ProveedorPagoRepositorio proveedores,
                                         List<ProveedorPagosPort> adaptadores,
                                         ServicioEstadoPagoCheckout estados,
                                         ObjectMapper objectMapper) {
        this.eventos = eventos;
        this.intentos = intentos;
        this.proveedores = proveedores;
        this.adaptadores = adaptadores;
        this.estados = estados;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${aplicacion.pagos.checkout.intervalo-webhooks-ms:5000}")
    public void procesarPendientes() {
        eventos.findTop50ByEstadoProcesamientoOrderByRecibidoEnAsc("RECIBIDO").forEach(this::procesar);
    }

    private void procesar(EventoWebhookPagoEntidad evento) {
        evento.setIntentos(evento.getIntentos() + 1);
        try {
            String referencia = referencia(evento.getPayload());
            IntentoPagoCheckoutEntidad intento = intentos
                    .findByProveedorPagoIdAndReferenciaExterna(evento.getProveedorPagoId(), referencia)
                    .orElse(null);
            if (intento == null) {
                evento.setEstadoProcesamiento("IGNORADO");
                evento.setErrorUltimo("El evento no corresponde a una orden de checkout conocida");
            } else {
                ProveedorPagoEntidad proveedor = proveedores.findById(evento.getProveedorPagoId())
                        .orElseThrow(() -> new IllegalStateException("Proveedor de pago no encontrado"));
                ProveedorPago codigo = ProveedorPago.valueOf(proveedor.getCodigo());
                ProveedorPagosPort adaptador = adaptadores.stream().filter(item -> item.proveedor() == codigo)
                        .findFirst().orElseThrow(() -> new IllegalStateException("Adaptador de pago no encontrado"));
                estados.aplicar(intento.getId(), adaptador.consultarCobro(intento.getEmpresaId(), referencia));
                evento.setEstadoProcesamiento("PROCESADO");
                evento.setErrorUltimo(null);
            }
            evento.setProcesadoEn(LocalDateTime.now(ZoneOffset.UTC));
        } catch (Exception ex) {
            evento.setEstadoProcesamiento(evento.getIntentos() >= 5 ? "ERROR" : "RECIBIDO");
            evento.setErrorUltimo(limitar(ex.getMessage(), 1000));
            LOGGER.warn("No fue posible procesar el webhook de pago {}", evento.getEventoExternoId(), ex);
        }
        eventos.save(evento);
    }

    private String referencia(String payload) throws Exception {
        JsonNode cuerpo = objectMapper.readTree(payload);
        String referencia = cuerpo.path("data").path("id").asText(null);
        if (referencia == null || referencia.isBlank()) throw new IllegalArgumentException("El webhook no contiene data.id");
        return referencia;
    }

    private String limitar(String valor, int maximo) {
        if (valor == null) return "Error sin detalle";
        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }
}
