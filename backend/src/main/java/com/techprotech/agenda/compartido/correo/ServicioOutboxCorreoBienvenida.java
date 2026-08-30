package com.techprotech.agenda.compartido.correo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ServicioOutboxCorreoBienvenida {

    private final BandejaSalidaNotificacionRepositorio repositorio;
    private final ClienteCorreoSendgrid correoSendgrid;
    private final ObjectMapper objectMapper;

    public ServicioOutboxCorreoBienvenida(
            BandejaSalidaNotificacionRepositorio repositorio,
            ClienteCorreoSendgrid correoSendgrid,
            ObjectMapper objectMapper
    ) {
        this.repositorio = repositorio;
        this.correoSendgrid = correoSendgrid;
        this.objectMapper = objectMapper;
    }

    public boolean programar(BienvenidaEmpresaCorreo bienvenida) {
        if (!correoSendgrid.configuracionCanonica().habilitado()) {
            return false;
        }
        if (repositorio.existsByAgregadoIdAndCanalAndTipoEventoAndEstadoIn(
                bienvenida.empresaId(), "EMAIL", "EMPRESA_BIENVENIDA_EMAIL",
                List.of("PENDIENTE", "PROCESANDO", "ENVIADA"))) {
            return true;
        }

        BandejaSalidaNotificacionEntidad salida = new BandejaSalidaNotificacionEntidad();
        salida.setEmpresaId(bienvenida.empresaId());
        salida.setTipoAgregado("EMPRESA");
        salida.setAgregadoId(bienvenida.empresaId());
        salida.setTipoEvento("EMPRESA_BIENVENIDA_EMAIL");
        salida.setCanal("EMAIL");
        salida.setPayloadJson(serializar(bienvenida));
        salida.setEstado("PENDIENTE");
        salida.setProgramadaEn(LocalDateTime.now());
        salida.setIntentos(0);
        repositorio.save(salida);
        return true;
    }

    private String serializar(BienvenidaEmpresaCorreo bienvenida) {
        try {
            return objectMapper.writeValueAsString(bienvenida);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo programar el correo de bienvenida", ex);
        }
    }
}
