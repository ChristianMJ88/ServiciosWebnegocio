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

    private String serializar(ConfirmacionCitaCorreo confirmacion) {
        try {
            return objectMapper.writeValueAsString(confirmacion);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar la confirmacion de cita", ex);
        }
    }
}
