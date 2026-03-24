package com.techprotech.agenda.compartido.correo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ServicioOutboxCorreoContactos {

    private final BandejaSalidaNotificacionRepositorio bandejaSalidaNotificacionRepositorio;
    private final ServicioConfiguracionCorreoEmpresa servicioConfiguracionCorreoEmpresa;
    private final ObjectMapper objectMapper;

    public ServicioOutboxCorreoContactos(
            BandejaSalidaNotificacionRepositorio bandejaSalidaNotificacionRepositorio,
            ServicioConfiguracionCorreoEmpresa servicioConfiguracionCorreoEmpresa,
            ObjectMapper objectMapper
    ) {
        this.bandejaSalidaNotificacionRepositorio = bandejaSalidaNotificacionRepositorio;
        this.servicioConfiguracionCorreoEmpresa = servicioConfiguracionCorreoEmpresa;
        this.objectMapper = objectMapper;
    }

    public boolean programarNotificacion(Long empresaId, NotificacionSolicitudContactoCorreo notificacion) {
        if (!servicioConfiguracionCorreoEmpresa.resolver(empresaId).habilitado()) {
            return false;
        }

        BandejaSalidaNotificacionEntidad salida = new BandejaSalidaNotificacionEntidad();
        salida.setEmpresaId(empresaId);
        salida.setTipoAgregado("CONTACTO");
        salida.setAgregadoId(notificacion.solicitudContactoId());
        salida.setTipoEvento("CONTACTO_REGISTRADO");
        salida.setCanal("EMAIL");
        salida.setPayloadJson(serializar(notificacion));
        salida.setEstado("PENDIENTE");
        salida.setProgramadaEn(LocalDateTime.now());
        salida.setIntentos(0);
        bandejaSalidaNotificacionRepositorio.save(salida);
        return true;
    }

    private String serializar(NotificacionSolicitudContactoCorreo notificacion) {
        try {
            return objectMapper.writeValueAsString(notificacion);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar la solicitud de contacto", ex);
        }
    }
}
