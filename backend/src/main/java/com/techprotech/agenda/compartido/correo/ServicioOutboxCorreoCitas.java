package com.techprotech.agenda.compartido.correo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ServicioOutboxCorreoCitas {

    private final BandejaSalidaNotificacionRepositorio bandejaSalidaNotificacionRepositorio;
    private final ServicioConfiguracionCorreoEmpresa servicioConfiguracionCorreoEmpresa;
    private final ObjectMapper objectMapper;

    public ServicioOutboxCorreoCitas(
            BandejaSalidaNotificacionRepositorio bandejaSalidaNotificacionRepositorio,
            ServicioConfiguracionCorreoEmpresa servicioConfiguracionCorreoEmpresa,
            ObjectMapper objectMapper
    ) {
        this.bandejaSalidaNotificacionRepositorio = bandejaSalidaNotificacionRepositorio;
        this.servicioConfiguracionCorreoEmpresa = servicioConfiguracionCorreoEmpresa;
        this.objectMapper = objectMapper;
    }

    public boolean programarConfirmacion(Long empresaId, ConfirmacionCitaCorreo confirmacion) {
        if (!servicioConfiguracionCorreoEmpresa.resolver(empresaId).habilitado()) {
            return false;
        }

        BandejaSalidaNotificacionEntidad salida = new BandejaSalidaNotificacionEntidad();
        salida.setEmpresaId(empresaId);
        salida.setTipoAgregado("CITA");
        salida.setAgregadoId(confirmacion.citaId());
        salida.setTipoEvento("CITA_CONFIRMADA");
        salida.setCanal("EMAIL");
        salida.setPayloadJson(serializar(confirmacion));
        salida.setEstado("PENDIENTE");
        salida.setProgramadaEn(LocalDateTime.now());
        salida.setIntentos(0);
        bandejaSalidaNotificacionRepositorio.save(salida);
        return true;
    }

    public boolean programarRegistrada(Long empresaId, Long citaId, LocalDateTime inicioEsperado) {
        return programar(empresaId, citaId, "CITA_REGISTRADA_EMAIL", inicioEsperado, LocalDateTime.now());
    }

    public boolean programarConfirmada(Long empresaId, Long citaId, LocalDateTime inicioEsperado) {
        return programar(empresaId, citaId, "CITA_CONFIRMADA_EMAIL", inicioEsperado, LocalDateTime.now());
    }

    public boolean programarReprogramada(Long empresaId, Long citaId, LocalDateTime inicioEsperado) {
        return programar(empresaId, citaId, "CITA_REPROGRAMADA_EMAIL", inicioEsperado, LocalDateTime.now());
    }

    public boolean programarRecordatorioConfirmacion(Long empresaId, Long citaId, LocalDateTime inicioEsperado) {
        return programar(empresaId, citaId, "CITA_RECORDATORIO_CONFIRMACION_EMAIL", inicioEsperado, LocalDateTime.now());
    }

    public boolean programarRecordatorio(Long empresaId, Long citaId, LocalDateTime inicioEsperado) {
        return programar(empresaId, citaId, "CITA_RECORDATORIO_EMAIL", inicioEsperado, LocalDateTime.now());
    }

    public boolean programarCanceladaCliente(Long empresaId, Long citaId, LocalDateTime inicioEsperado) {
        return programar(empresaId, citaId, "CITA_CANCELADA_CLIENTE_EMAIL", inicioEsperado, LocalDateTime.now());
    }

    public boolean programarCanceladaNegocio(Long empresaId, Long citaId, LocalDateTime inicioEsperado) {
        return programar(empresaId, citaId, "CITA_CANCELADA_NEGOCIO_EMAIL", inicioEsperado, LocalDateTime.now());
    }

    public boolean programarLiberadaSinConfirmacion(Long empresaId, Long citaId, LocalDateTime inicioEsperado) {
        return programar(empresaId, citaId, "CITA_LIBERADA_SIN_CONFIRMACION_EMAIL", inicioEsperado, LocalDateTime.now());
    }

    public boolean programarGraciasVisita(Long empresaId, Long citaId, LocalDateTime inicioEsperado) {
        return programar(empresaId, citaId, "CITA_GRACIAS_VISITA_EMAIL", inicioEsperado, LocalDateTime.now());
    }

    public boolean programarNoAsistio(Long empresaId, Long citaId, LocalDateTime inicioEsperado) {
        return programar(empresaId, citaId, "CITA_NO_ASISTIO_EMAIL", inicioEsperado, LocalDateTime.now());
    }

    private boolean programar(Long empresaId, Long citaId, String evento, LocalDateTime inicioEsperado, LocalDateTime programadaEn) {
        if (!servicioConfiguracionCorreoEmpresa.resolver(empresaId).habilitado()) {
            return false;
        }
        if (bandejaSalidaNotificacionRepositorio.existsByAgregadoIdAndCanalAndTipoEventoAndEstadoIn(
                citaId, "EMAIL", evento, java.util.List.of("PENDIENTE", "PROCESANDO", "ENVIADA"))) {
            return true;
        }
        BandejaSalidaNotificacionEntidad salida = new BandejaSalidaNotificacionEntidad();
        salida.setEmpresaId(empresaId);
        salida.setTipoAgregado("CITA");
        salida.setAgregadoId(citaId);
        salida.setTipoEvento(evento);
        salida.setCanal("EMAIL");
        salida.setPayloadJson(serializar(new EventoCitaCorreoPayload(citaId, inicioEsperado)));
        salida.setEstado("PENDIENTE");
        salida.setProgramadaEn(programadaEn);
        salida.setIntentos(0);
        bandejaSalidaNotificacionRepositorio.save(salida);
        return true;
    }

    private String serializar(ConfirmacionCitaCorreo confirmacion) {
        try {
            return objectMapper.writeValueAsString(confirmacion);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar la confirmacion de cita", ex);
        }
    }

    private String serializar(EventoCitaCorreoPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar el evento de correo de cita", ex);
        }
    }
}
