package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class LiberadorReservasCheckout {
    private static final Logger LOGGER = LoggerFactory.getLogger(LiberadorReservasCheckout.class);

    private final CheckoutOrdenRepositorio ordenes;
    private final ServicioInventarioCheckout inventario;

    public LiberadorReservasCheckout(CheckoutOrdenRepositorio ordenes, ServicioInventarioCheckout inventario) {
        this.ordenes = ordenes;
        this.inventario = inventario;
    }

    @Scheduled(fixedDelayString = "${aplicacion.pagos.checkout.intervalo-liberacion-ms:60000}")
    public void liberarExpiradas() {
        ordenes.findTop100ByInventarioEstadoAndExpiraEnBeforeOrderByExpiraEnAsc(
                "RESERVADO", LocalDateTime.now(ZoneOffset.UTC)).forEach(orden -> {
            try {
                inventario.expirar(orden.getEmpresaId(), orden.getId());
            } catch (RuntimeException ex) {
                LOGGER.warn("No fue posible liberar la reserva de la orden de checkout {}", orden.getPublicId(), ex);
            }
        });
    }
}
