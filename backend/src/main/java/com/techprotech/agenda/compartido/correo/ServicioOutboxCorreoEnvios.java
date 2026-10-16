package com.techprotech.agenda.compartido.correo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ServicioOutboxCorreoEnvios {
    private final java.time.Clock reloj;
    private static final Map<String, String> EVENTOS = Map.of(
            "GUIA_GENERADA", "ENVIO_GUIA_GENERADA_EMAIL",
            "EN_TRANSITO", "ENVIO_EN_TRANSITO_EMAIL",
            "ENTREGADO", "ENVIO_ENTREGADO_EMAIL",
            "INCIDENCIA", "ENVIO_INCIDENCIA_EMAIL"
    );
    private final BandejaSalidaNotificacionRepositorio outbox;
    private final ServicioConfiguracionCorreoEmpresa configuracionCorreo;
    private final ObjectMapper objectMapper;

    public ServicioOutboxCorreoEnvios(BandejaSalidaNotificacionRepositorio outbox,
                                      ServicioConfiguracionCorreoEmpresa configuracionCorreo,
                                      ObjectMapper objectMapper, java.time.Clock reloj) {
        this.reloj = reloj;
        this.outbox = outbox;
        this.configuracionCorreo = configuracionCorreo;
        this.objectMapper = objectMapper;
    }

    public boolean programar(Long empresaId, Long envioId, String estado) {
        String tipoEvento = EVENTOS.get(estado);
        if (tipoEvento == null || !configuracionCorreo.resolver(empresaId).habilitado()) return false;
        if (outbox.existsByAgregadoIdAndCanalAndTipoEventoAndEstadoIn(
                envioId, "EMAIL", tipoEvento, List.of("PENDIENTE", "PROCESANDO", "ENVIADA"))) return true;
        BandejaSalidaNotificacionEntidad salida = new BandejaSalidaNotificacionEntidad();
        salida.setEmpresaId(empresaId);
        salida.setTipoAgregado("ENVIO");
        salida.setAgregadoId(envioId);
        salida.setTipoEvento(tipoEvento);
        salida.setCanal("EMAIL");
        salida.setPayloadJson(serializar(new EventoEnvioCorreoPayload(envioId)));
        salida.setEstado("PENDIENTE");
        salida.setProgramadaEn(LocalDateTime.now(reloj));
        salida.setIntentos(0);
        outbox.save(salida);
        return true;
    }

    private String serializar(EventoEnvioCorreoPayload payload) {
        try { return objectMapper.writeValueAsString(payload); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("No se pudo serializar el aviso de envío", ex); }
    }
}
