package com.techprotech.agenda.modulos.envios.aplicacion;

import com.techprotech.agenda.modulos.envios.infraestructura.repositorio.EnvioPedidoRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProcesadorGuiasYRastreoEnvio {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcesadorGuiasYRastreoEnvio.class);
    private final EnvioPedidoRepositorio envios;
    private final ServicioGuiasEnvio servicio;

    public ProcesadorGuiasYRastreoEnvio(EnvioPedidoRepositorio envios, ServicioGuiasEnvio servicio) {
        this.envios = envios; this.servicio = servicio;
    }

    @Scheduled(fixedDelayString = "${aplicacion.envios.intervalo-generacion-guias-ms:10000}")
    public void generarGuiasAutomaticas() {
        envios.findTop50ByEstadoOrderByActualizadoEnAsc("LISTO_PARA_GUIA").forEach(envio -> {
            try { servicio.generarAutomaticamente(envio); }
            catch (Exception ex) { LOGGER.warn("No fue posible generar la guía del envío {}", envio.getId(), ex); }
        });
    }

    @Scheduled(fixedDelayString = "${aplicacion.envios.intervalo-rastreo-ms:900000}")
    public void actualizarRastreos() {
        envios.findTop50ByEstadoInAndNumeroGuiaIsNotNullOrderByRastreoActualizadoEnAsc(
                List.of("GUIA_GENERADA", "EN_TRANSITO", "INCIDENCIA")).forEach(servicio::actualizarRastreo);
    }
}
