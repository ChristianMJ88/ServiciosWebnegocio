package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.modulos.pagos.dominio.EstadoPago;
import com.techprotech.agenda.modulos.pagos.dominio.ResultadoCobro;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.IntentoPagoCheckoutEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.IntentoPagoCheckoutRepositorio;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.*;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioEstadoPagoCheckout {
    private final IntentoPagoCheckoutRepositorio intentos;
    private final CheckoutOrdenRepositorio ordenes;
    private final ServicioInventarioCheckout inventario;
    private final ServicioConfirmacionCitasCheckout citas;
    private final ServicioConciliacionPagos conciliacion;
    private final EntityManager entityManager;
    private final Clock reloj;

    private final com.techprotech.agenda.modulos.pagos.aplicacion.ServicioAceptacionPoliticaCheckout aceptacionPolitica;

    public ServicioEstadoPagoCheckout(IntentoPagoCheckoutRepositorio intentos, CheckoutOrdenRepositorio ordenes,
            ServicioInventarioCheckout inventario, ServicioConfirmacionCitasCheckout citas,
            ServicioConciliacionPagos conciliacion, EntityManager entityManager, Clock reloj, com.techprotech.agenda.modulos.pagos.aplicacion.ServicioAceptacionPoliticaCheckout aceptacionPolitica) {
        this.aceptacionPolitica = aceptacionPolitica;
        this.intentos = intentos; this.ordenes = ordenes; this.inventario = inventario;
        this.citas = citas; this.conciliacion = conciliacion; this.entityManager = entityManager; this.reloj = reloj;
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public EstadoAplicado aplicar(Long intentoId, ResultadoCobro resultado) {
        var intento = intentos.findById(intentoId).orElseThrow(() -> new IllegalStateException("Intento no encontrado"));
        var orden = ordenes.bloquearPorEmpresaEId(intento.getEmpresaId(), intento.getCheckoutOrdenId())
                .orElseThrow(() -> new IllegalStateException("Orden no encontrada"));
        entityManager.refresh(orden, LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(intento, LockModeType.PESSIMISTIC_WRITE);
        if ("APROBADO".equals(intento.getEstado()) && Set.of("PAGADA", "PAGADA_CON_INCIDENCIA").contains(orden.getEstado())) {
            // Mantener la resolución operativa y actualizar los datos financieros del proveedor.
            if (resultado.estado() == EstadoPago.APROBADO) conciliacion.registrarCheckout(intento, resultado);
            return new EstadoAplicado(orden.getPublicId(), orden.getEstado(), intento);
        }
        if ("REEMBOLSADA".equals(orden.getEstado())) return new EstadoAplicado(orden.getPublicId(), orden.getEstado(), intento);
        if (("APROBADO".equals(intento.getEstado()) || Set.of("PAGADA", "PAGADA_CON_INCIDENCIA").contains(orden.getEstado()))
                && resultado.estado() != EstadoPago.APROBADO) return new EstadoAplicado(orden.getPublicId(), orden.getEstado(), intento);

        intento.setReferenciaExterna(resultado.referenciaExterna());
        intento.setReferenciaPagoExterna(resultado.referenciaPagoExterna());
        intento.setEstado(resultado.estado().name()); intento.setAccionUrl(resultado.accionUrl());
        intento.setCodigoRespuesta(resultado.codigoRespuesta());
        intento.setDetalleRespuesta(limitar(resultado.detalleRespuesta(), 500));
        Long empresa = intento.getEmpresaId(); Long id = intento.getCheckoutOrdenId();
        if (resultado.estado() == EstadoPago.APROBADO) {
            boolean stock = inventario.confirmarSiDisponible(empresa, id);
            aceptacionPolitica.acreditarAceptacion(orden);
            boolean reserva = stock && citas.confirmar(empresa, id);
            if (!reserva) {
                orden.setRequiereConciliacion(true);
                orden.setMotivoIncidencia(stock ? "Pago aprobado: el grupo no puede confirmarse; requiere reprogramación o revisión del negocio"
                        : "Pago aprobado: inventario no disponible; requiere revisión del negocio");
                citas.avisarIncidencia(empresa, id);
            }
            orden.setEstado(orden.isRequiereConciliacion() ? "PAGADA_CON_INCIDENCIA" : "PAGADA");
        } else if (resultado.estado() == EstadoPago.CANCELADO) {
            inventario.liberar(empresa, id); citas.liberar(empresa, id); orden.setEstado("CANCELADA");
        } else if (resultado.estado() == EstadoPago.RECHAZADO) {
            if (!"EXPIRADA".equals(orden.getEstado()) && orden.getExpiraEn().isAfter(LocalDateTime.now(reloj))) {
                orden.setEstado("PENDIENTE_PAGO"); // D6: otro intento dentro del apartado original.
            } else {
                inventario.expirar(empresa, id); citas.liberar(empresa, id); orden.setEstado("EXPIRADA");
            }
        } else if (resultado.estado() == EstadoPago.PROCESANDO || resultado.estado() == EstadoPago.REQUIERE_ACCION) {
            if (!"EXPIRADA".equals(orden.getEstado()) && !"CANCELADA".equals(orden.getEstado())) {
                OffsetDateTime limite = resultado.expiraEn() != null ? resultado.expiraEn()
                        : intento.getPagoExpiraEn() == null ? null : intento.getPagoExpiraEn().atOffset(ZoneOffset.UTC);
                limite = citas.prorrogar(empresa, id, limite);
                var fecha = limite.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
                intento.setPagoExpiraEn(fecha); orden.setExpiraEn(fecha); orden.setEstado("PAGO_EN_PROCESO");
            }
        }
        intentos.save(intento); ordenes.save(orden);
        // Incidencias operativas no descartan ni revierten el pago aprobado.
        conciliacion.registrarCheckout(intento, resultado);
        return new EstadoAplicado(orden.getPublicId(), orden.getEstado(), intento);
    }

    private String limitar(String valor, int maximo) {
        return valor == null || valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }
    public record EstadoAplicado(String ordenPublicId, String estadoOrden, IntentoPagoCheckoutEntidad intento) {}
}
