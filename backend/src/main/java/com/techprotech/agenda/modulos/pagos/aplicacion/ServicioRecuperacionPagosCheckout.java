package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.modulos.disponibilidad.aplicacion.ServicioConfiguracionReservas;
import com.techprotech.agenda.modulos.pagos.dominio.*;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.*;
import java.time.*;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/** Consulta el proveedor fuera de la transacción; después aplica/expira bajo bloqueo. */
@Service
public class ServicioRecuperacionPagosCheckout {
    private static final Logger LOGGER=LoggerFactory.getLogger(ServicioRecuperacionPagosCheckout.class);
    private final IntentoPagoCheckoutRepositorio intentos;
    private final ProveedorPagoRepositorio proveedores;
    private final List<ProveedorPagosPort> adaptadores;
    private final ServicioEstadoPagoCheckout estados;
    private final ServicioExpiracionCheckout expiracion;
    private final ServicioConfiguracionReservas reservas;
    private final Clock reloj;

    public ServicioRecuperacionPagosCheckout(IntentoPagoCheckoutRepositorio intentos, ProveedorPagoRepositorio proveedores,
            List<ProveedorPagosPort> adaptadores, ServicioEstadoPagoCheckout estados,
            ServicioExpiracionCheckout expiracion, ServicioConfiguracionReservas reservas, Clock reloj) {
        this.intentos=intentos; this.proveedores=proveedores; this.adaptadores=adaptadores;
        this.estados=estados; this.expiracion=expiracion; this.reservas=reservas; this.reloj=reloj;
    }

    public void recuperar() {
        var ahora=LocalDateTime.now(reloj);
        for (Long id:intentos.buscarIdsParaRecuperacion(ahora,PageRequest.of(0,100))) {
            var existente=intentos.findById(id);
            if (existente.isEmpty()) continue;
            var intento=existente.get();
            var limite=intento.getPagoExpiraEn();
            if (limite==null) {
                // Intentos anteriores a esta corrección también tienen un tope, desde su creación.
                if (intento.getCreadoEn()==null) continue;
                limite=intento.getCreadoEn().plusHours(reservas.obtener(intento.getEmpresaId()).horasTopeProcesamientoPago());
            }
            if (limite.isAfter(ahora)) continue;
            try {
                var proveedor=proveedores.findById(intento.getProveedorPagoId()).orElseThrow();
                var adaptador=adaptadores.stream().filter(a -> a.proveedor().name().equals(proveedor.getCodigo())).findFirst().orElseThrow();
                var resultado=adaptador.consultarCobro(intento.getEmpresaId(),intento.getReferenciaExterna());
                // Nunca inventar CANCELADO/RECHAZADO si el proveedor todavía procesa.
                if ((resultado.estado()==EstadoPago.PROCESANDO || resultado.estado()==EstadoPago.REQUIERE_ACCION) && resultado.expiraEn()==null) {
                    resultado=new ResultadoCobro(resultado.proveedor(),resultado.referenciaExterna(),resultado.referenciaPagoExterna(),resultado.estado(),resultado.accionUrl(),resultado.codigoRespuesta(),resultado.detalleRespuesta(),resultado.actualizadoEn(),resultado.liquidacion(),limite.atOffset(ZoneOffset.UTC));
                }
                estados.aplicar(id,resultado);
            } catch (RuntimeException ex) {
                LOGGER.warn("No fue posible consultar el intento {} antes de vencer el tope de procesamiento",id);
            }
            expiracion.expirarEnProceso(intento.getEmpresaId(),intento.getCheckoutOrdenId(),id,limite);
        }
    }
}
