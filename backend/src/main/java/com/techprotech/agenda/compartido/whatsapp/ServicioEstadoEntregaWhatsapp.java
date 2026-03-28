package com.techprotech.agenda.compartido.whatsapp;

import com.techprotech.agenda.compartido.correo.BandejaSalidaNotificacionEntidad;
import com.techprotech.agenda.compartido.correo.BandejaSalidaNotificacionRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ServicioEstadoEntregaWhatsapp {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicioEstadoEntregaWhatsapp.class);

    private final BandejaSalidaNotificacionRepositorio bandejaSalidaNotificacionRepositorio;

    public ServicioEstadoEntregaWhatsapp(BandejaSalidaNotificacionRepositorio bandejaSalidaNotificacionRepositorio) {
        this.bandejaSalidaNotificacionRepositorio = bandejaSalidaNotificacionRepositorio;
    }

    @Transactional
    public void registrarActualizacion(
            String proveedorMensajeId,
            String estadoEntrega,
            String codigoErrorProveedor,
            String detalleErrorProveedor
    ) {
        if (!tieneTexto(proveedorMensajeId)) {
            LOGGER.warn("Se recibio callback de estado de WhatsApp sin MessageSid");
            return;
        }

        BandejaSalidaNotificacionEntidad salida = bandejaSalidaNotificacionRepositorio
                .findFirstByCanalAndProveedorMensajeId("WHATSAPP", proveedorMensajeId)
                .orElse(null);

        if (salida == null) {
            LOGGER.warn("No se encontro outbox de WhatsApp para MessageSid {}", proveedorMensajeId);
            return;
        }

        salida.setEstadoEntrega(estadoEntrega);
        salida.setEstadoEntregaActualizadoEn(LocalDateTime.now());
        salida.setCodigoErrorProveedor(normalizar(codigoErrorProveedor, 32));
        salida.setDetalleErrorProveedor(normalizar(detalleErrorProveedor, 500));

        if (esEstadoFallo(estadoEntrega) && !tieneTexto(salida.getMensajeError())) {
            salida.setMensajeError(normalizar(detalleErrorProveedor, 500));
        }

        bandejaSalidaNotificacionRepositorio.save(salida);
    }

    private boolean esEstadoFallo(String estadoEntrega) {
        if (estadoEntrega == null) {
            return false;
        }
        return "failed".equalsIgnoreCase(estadoEntrega) || "undelivered".equalsIgnoreCase(estadoEntrega);
    }

    private boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private String normalizar(String valor, int maximo) {
        if (!tieneTexto(valor)) {
            return null;
        }
        return valor.length() > maximo ? valor.substring(0, maximo) : valor;
    }
}
