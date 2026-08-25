package com.techprotech.agenda.compartido.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.compartido.correo.BandejaSalidaNotificacionEntidad;
import com.techprotech.agenda.compartido.correo.BandejaSalidaNotificacionRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ServicioOutboxWhatsappCitas {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicioOutboxWhatsappCitas.class);
    private static final String CANAL = "WHATSAPP";
    private static final String TIPO_AGREGADO = "CITA";

    private final BandejaSalidaNotificacionRepositorio bandejaSalidaNotificacionRepositorio;
    private final ClienteWhatsappTwilio clienteWhatsappTwilio;
    private final ObjectMapper objectMapper;

    public ServicioOutboxWhatsappCitas(
            BandejaSalidaNotificacionRepositorio bandejaSalidaNotificacionRepositorio,
            ClienteWhatsappTwilio clienteWhatsappTwilio,
            ObjectMapper objectMapper
    ) {
        this.bandejaSalidaNotificacionRepositorio = bandejaSalidaNotificacionRepositorio;
        this.clienteWhatsappTwilio = clienteWhatsappTwilio;
        this.objectMapper = objectMapper;
    }

    public void programarConfirmacion(Long empresaId, Long citaId, String telefonoDestino, LocalDateTime inicioEsperado) {
      programarConfirmacion(empresaId, citaId, telefonoDestino, inicioEsperado, null);
    }

    public void programarConfirmacion(Long empresaId, Long citaId, String telefonoDestino, LocalDateTime inicioEsperado, Integer totalServicios) {
      programar(
        empresaId,
        citaId,
        "CITA_REGISTRADA_WHATSAPP",
        telefonoDestino,
        inicioEsperado,
        LocalDateTime.now(),
        totalServicios
      );
    }

    public void programarCitaConfirmada(Long empresaId, Long citaId, String telefonoDestino, LocalDateTime inicioEsperado) {
      programar(
        empresaId,
        citaId,
        "CITA_CONFIRMADA_WHATSAPP",
        telefonoDestino,
        inicioEsperado,
        LocalDateTime.now(),
        null
      );
    }

    public void programarCitaReprogramadaPendiente(Long empresaId, Long citaId, String telefonoDestino, LocalDateTime inicioEsperado) {
      programar(
        empresaId,
        citaId,
        "CITA_REPROGRAMADA_PENDIENTE_WHATSAPP",
        telefonoDestino,
        inicioEsperado,
        LocalDateTime.now(),
        null
      );
    }

    public void programarRecordatorioConfirmacion(Long empresaId, Long citaId, String telefonoDestino, LocalDateTime inicioEsperado, LocalDateTime programadaEn) {
      programar(
        empresaId,
        citaId,
        "CITA_RECORDATORIO_CONFIRMACION_WHATSAPP",
        telefonoDestino,
        inicioEsperado,
        programadaEn,
        null
      );
    }

    public void programarRecordatorio(Long empresaId, Long citaId, String telefonoDestino, LocalDateTime inicioEsperado, LocalDateTime programadaEn) {
      programar(
        empresaId,
        citaId,
        "CITA_RECORDATORIO_WHATSAPP",
        telefonoDestino,
        inicioEsperado,
        programadaEn,
        null
      );
    }

    public void programarCancelacionNegocio(Long empresaId, Long citaId, String telefonoDestino, LocalDateTime inicioEsperado) {
      programar(
        empresaId,
        citaId,
        "CITA_CANCELADA_NEGOCIO_WHATSAPP",
        telefonoDestino,
        inicioEsperado,
        LocalDateTime.now(),
        null
      );
    }

    public void programarLiberadaSinConfirmacion(Long empresaId, Long citaId, String telefonoDestino, LocalDateTime inicioEsperado) {
      programar(
        empresaId,
        citaId,
        "CITA_LIBERADA_SIN_CONFIRMACION_WHATSAPP",
        telefonoDestino,
        inicioEsperado,
        LocalDateTime.now(),
        null
      );
    }

    public void programarGraciasVisita(Long empresaId, Long citaId, String telefonoDestino, LocalDateTime inicioEsperado) {
      programar(
        empresaId,
        citaId,
        "CITA_GRACIAS_VISITA_WHATSAPP",
        telefonoDestino,
        inicioEsperado,
        LocalDateTime.now(),
        null
      );
    }

    private void programar(
            Long empresaId,
            Long citaId,
            String tipoEvento,
            String telefonoDestino,
            LocalDateTime inicioEsperado,
            LocalDateTime programadaEn,
            Integer totalServicios
    ) {
        if (!clienteWhatsappTwilio.estaHabilitado(empresaId)) {
            LOGGER.warn(
                    "No se encolo WhatsApp para cita {} evento {} empresa {}: {}",
                    citaId,
                    tipoEvento,
                    empresaId,
                    clienteWhatsappTwilio.diagnosticoConfiguracion(empresaId)
            );
            return;
        }

        if (telefonoDestino == null || telefonoDestino.isBlank()) {
            LOGGER.warn(
                    "No se encolo WhatsApp para cita {} evento {} empresa {}: telefono destino vacio",
                    citaId,
                    tipoEvento,
                    empresaId
            );
            return;
        }

        BandejaSalidaNotificacionEntidad salida = new BandejaSalidaNotificacionEntidad();
        salida.setEmpresaId(empresaId);
        salida.setTipoAgregado(TIPO_AGREGADO);
        salida.setAgregadoId(citaId);
        salida.setTipoEvento(tipoEvento);
        salida.setCanal(CANAL);
        salida.setPayloadJson(serializar(new MensajeCitaWhatsappPayload(citaId, telefonoDestino, inicioEsperado, totalServicios)));
        salida.setEstado("PENDIENTE");
        salida.setProgramadaEn(programadaEn);
        salida.setIntentos(0);
        bandejaSalidaNotificacionRepositorio.save(salida);
        LOGGER.info(
                "WhatsApp encolado para cita {} evento {} empresa {} telefono {}",
                citaId,
                tipoEvento,
                empresaId,
                enmascararTelefono(telefonoDestino)
        );
    }

    private String enmascararTelefono(String telefono) {
        String normalizado = telefono == null ? "" : telefono.replaceAll("\\s+", "");
        if (normalizado.length() <= 4) {
            return normalizado;
        }
        return "*".repeat(Math.max(0, normalizado.length() - 4)) + normalizado.substring(normalizado.length() - 4);
    }

    private String serializar(MensajeCitaWhatsappPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar la notificacion de WhatsApp", ex);
        }
    }
}
