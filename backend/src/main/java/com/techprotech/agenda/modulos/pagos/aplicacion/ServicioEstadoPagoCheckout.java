package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.modulos.pagos.dominio.EstadoPago;
import com.techprotech.agenda.modulos.pagos.dominio.ResultadoCobro;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.IntentoPagoCheckoutEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.IntentoPagoCheckoutRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioEstadoPagoCheckout {
    private final IntentoPagoCheckoutRepositorio intentos;
    private final CheckoutOrdenRepositorio ordenes;
    private final ServicioInventarioCheckout inventario;

    public ServicioEstadoPagoCheckout(IntentoPagoCheckoutRepositorio intentos, CheckoutOrdenRepositorio ordenes,
                                      ServicioInventarioCheckout inventario) {
        this.intentos = intentos;
        this.ordenes = ordenes;
        this.inventario = inventario;
    }

    @Transactional
    public EstadoAplicado aplicar(Long intentoId, ResultadoCobro resultado) {
        IntentoPagoCheckoutEntidad intento = intentos.findById(intentoId)
                .orElseThrow(() -> new IllegalStateException("Intento de pago no encontrado"));
        intento.setReferenciaExterna(resultado.referenciaExterna());
        intento.setReferenciaPagoExterna(resultado.referenciaPagoExterna());
        intento.setEstado(resultado.estado().name());
        intento.setAccionUrl(resultado.accionUrl());
        intento.setCodigoRespuesta(resultado.codigoRespuesta());
        intento.setDetalleRespuesta(limitar(resultado.detalleRespuesta(), 500));
        intentos.save(intento);

        if (resultado.estado() == EstadoPago.APROBADO) {
            inventario.confirmar(intento.getEmpresaId(), intento.getCheckoutOrdenId());
        } else if (resultado.estado() == EstadoPago.RECHAZADO || resultado.estado() == EstadoPago.CANCELADO) {
            inventario.liberar(intento.getEmpresaId(), intento.getCheckoutOrdenId());
        }

        CheckoutOrdenEntidad orden = ordenes.findById(intento.getCheckoutOrdenId())
                .orElseThrow(() -> new IllegalStateException("Orden de checkout no encontrada"));
        orden.setEstado(estadoOrden(resultado.estado()));
        ordenes.save(orden);
        return new EstadoAplicado(orden.getPublicId(), orden.getEstado(), intento);
    }

    private String estadoOrden(EstadoPago estado) {
        return switch (estado) {
            case APROBADO -> "PAGADA";
            case CREADO, REQUIERE_ACCION, PROCESANDO -> "PAGO_EN_PROCESO";
            case CANCELADO -> "CANCELADA";
            default -> "PENDIENTE_PAGO";
        };
    }

    private String limitar(String valor, int maximo) {
        return valor == null || valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }

    public record EstadoAplicado(String ordenPublicId, String estadoOrden, IntentoPagoCheckoutEntidad intento) {}
}
