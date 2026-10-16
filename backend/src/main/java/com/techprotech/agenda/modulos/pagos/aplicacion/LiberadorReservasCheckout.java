package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class LiberadorReservasCheckout {
    private static final Logger LOGGER = LoggerFactory.getLogger(LiberadorReservasCheckout.class);

    private final CheckoutOrdenRepositorio ordenes;
    private final ServicioInventarioCheckout inventario;
    private final ServicioConfirmacionCitasCheckout citas;
    private final ServicioRecuperacionPagosCheckout recuperacion;
    private final java.time.Clock reloj;
    private final com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.IntentoPagoCheckoutRepositorio intentos;

    public LiberadorReservasCheckout(CheckoutOrdenRepositorio ordenes, ServicioInventarioCheckout inventario, ServicioConfirmacionCitasCheckout citas, com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.IntentoPagoCheckoutRepositorio intentos, ServicioRecuperacionPagosCheckout recuperacion, java.time.Clock reloj) {
        this.recuperacion=recuperacion; this.reloj=reloj;
        this.citas=citas;this.intentos=intentos;
        this.ordenes = ordenes;
        this.inventario = inventario;
    }

    @Scheduled(fixedDelayString = "${aplicacion.pagos.checkout.intervalo-liberacion-ms:60000}")
    public void liberarExpiradas() {
        recuperacion.recuperar();
        citas.liberarCitasVencidas();
        ordenes.findTop100ByInventarioEstadoAndExpiraEnBeforeOrderByExpiraEnAsc(
                "RESERVADO", LocalDateTime.now(reloj)).forEach(orden -> {
try {
    var intento=intentos.findFirstByEmpresaIdAndCheckoutOrdenIdOrderByIdDesc(orden.getEmpresaId(),orden.getId());
    if ("PAGO_EN_PROCESO".equals(orden.getEstado())) return; // Lo gestiona la recuperación con consulta al proveedor.

                inventario.expirar(orden.getEmpresaId(), orden.getId());
            } catch (RuntimeException ex) {
                LOGGER.warn("No fue posible liberar la reserva de la orden de checkout {}", orden.getPublicId(), ex);
            }
        });
    }
}
