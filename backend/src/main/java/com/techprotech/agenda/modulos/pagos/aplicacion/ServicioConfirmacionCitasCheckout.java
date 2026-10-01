package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.HistorialEstadoCitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.HistorialEstadoCitaRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenDetalleRepositorio;
import org.springframework.stereotype.Service;

@Service
public class ServicioConfirmacionCitasCheckout {
    private final CheckoutOrdenDetalleRepositorio detalles;
    private final CitaRepositorio citas;
    private final HistorialEstadoCitaRepositorio historial;

    public ServicioConfirmacionCitasCheckout(CheckoutOrdenDetalleRepositorio detalles, CitaRepositorio citas,
                                             HistorialEstadoCitaRepositorio historial) {
        this.detalles = detalles;
        this.citas = citas;
        this.historial = historial;
    }

    public void confirmar(Long empresaId, Long ordenId) {
        detalles.findByEmpresaIdAndCheckoutOrdenIdOrderByIdAsc(empresaId, ordenId).stream()
                .filter(detalle -> "SERVICIO".equals(detalle.getTipoConcepto()) && detalle.getCitaId() != null)
                .forEach(detalle -> confirmarCita(empresaId, detalle.getCitaId()));
    }

    private void confirmarCita(Long empresaId, Long citaId) {
        CitaEntidad cita = citas.findById(citaId)
                .filter(item -> empresaId.equals(item.getEmpresaId()))
                .orElseThrow(() -> new IllegalStateException("La cita pagada no pertenece al negocio"));
        if ("CONFIRMADA".equals(cita.getEstado())) return;
        if (!"PENDIENTE".equals(cita.getEstado())) {
            throw new IllegalStateException("La cita pagada ya no puede confirmarse desde el checkout");
        }
        cita.setEstado("CONFIRMADA");
        citas.save(cita);

        HistorialEstadoCitaEntidad cambio = new HistorialEstadoCitaEntidad();
        cambio.setCitaId(cita.getId());
        cambio.setEstadoAnterior("PENDIENTE");
        cambio.setEstadoNuevo("CONFIRMADA");
        cambio.setCambiadoPorUsuarioId(cita.getClienteId());
        cambio.setMotivo("Pago en línea aprobado");
        historial.save(cambio);
    }
}
