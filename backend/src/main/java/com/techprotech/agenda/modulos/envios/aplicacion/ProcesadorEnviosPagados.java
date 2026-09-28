package com.techprotech.agenda.modulos.envios.aplicacion;

import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ProcesadorEnviosPagados {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcesadorEnviosPagados.class);
    private final CheckoutOrdenRepositorio ordenes;
    private final ServicioEnviosCheckout servicio;

    public ProcesadorEnviosPagados(CheckoutOrdenRepositorio ordenes, ServicioEnviosCheckout servicio) {
        this.ordenes = ordenes;
        this.servicio = servicio;
    }

    @Scheduled(fixedDelayString = "${aplicacion.envios.intervalo-preparacion-ms:5000}")
    public void prepararPagados() {
        ordenes.findTop50ByEstadoAndModalidadEntregaOrderByIdAsc("PAGADA", "ENVIO_NACIONAL")
                .forEach(orden -> {
                    try { servicio.preparar(orden); }
                    catch (Exception ex) { LOGGER.warn("No fue posible preparar el envío de la orden {}", orden.getId(), ex); }
                });
    }
}
