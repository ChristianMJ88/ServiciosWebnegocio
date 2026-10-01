package com.techprotech.agenda.modulos.caja.aplicacion;

import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ProyectorVentasCheckout {
    private static final Logger LOG = LoggerFactory.getLogger(ProyectorVentasCheckout.class);
    private final CheckoutOrdenRepositorio ordenes;
    private final ServicioVentasCheckout servicio;

    public ProyectorVentasCheckout(CheckoutOrdenRepositorio ordenes, ServicioVentasCheckout servicio) {
        this.ordenes = ordenes;
        this.servicio = servicio;
    }

    @Scheduled(fixedDelayString = "${aplicacion.pagos.checkout.intervalo-proyeccion-ventas-ms:15000}")
    public void ejecutar() {
        ordenes.findTop100ByEstadoOrderByIdAsc("PAGADA").forEach(orden -> {
            try {
                servicio.proyectar(orden.getEmpresaId(), orden.getId());
            } catch (RuntimeException ex) {
                LOG.error("No fue posible proyectar la orden de checkout {} como venta", orden.getPublicId(), ex);
            }
        });
    }
}
