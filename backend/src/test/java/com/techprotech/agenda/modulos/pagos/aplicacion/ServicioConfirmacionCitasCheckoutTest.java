package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.HistorialEstadoCitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.HistorialEstadoCitaRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenDetalleEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenDetalleRepositorio;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class ServicioConfirmacionCitasCheckoutTest {
    @Test
    void confirmaLaCitaPagadaYRegistraHistorialUnaSolaVez() {
        CheckoutOrdenDetalleRepositorio detalles = mock(CheckoutOrdenDetalleRepositorio.class);
        CitaRepositorio citas = mock(CitaRepositorio.class);
        HistorialEstadoCitaRepositorio historial = mock(HistorialEstadoCitaRepositorio.class);
        CheckoutOrdenDetalleEntidad detalle = new CheckoutOrdenDetalleEntidad();
        detalle.setTipoConcepto("SERVICIO"); detalle.setCitaId(18L);
        when(detalles.findByEmpresaIdAndCheckoutOrdenIdOrderByIdAsc(7L, 30L)).thenReturn(List.of(detalle));
        CitaEntidad cita = new CitaEntidad();
        cita.setId(18L); cita.setEmpresaId(7L); cita.setClienteId(44L); cita.setEstado("PENDIENTE");
        when(citas.findById(18L)).thenReturn(Optional.of(cita));
        ServicioConfirmacionCitasCheckout servicio = new ServicioConfirmacionCitasCheckout(detalles, citas, historial);

        servicio.confirmar(7L, 30L);
        servicio.confirmar(7L, 30L);

        assertThat(cita.getEstado()).isEqualTo("CONFIRMADA");
        verify(citas, times(1)).save(cita);
        verify(historial, times(1)).save(argThat((HistorialEstadoCitaEntidad cambio) ->
                "PENDIENTE".equals(cambio.getEstadoAnterior())
                        && "CONFIRMADA".equals(cambio.getEstadoNuevo())
                        && cambio.getCambiadoPorUsuarioId().equals(44L)));
    }
}
