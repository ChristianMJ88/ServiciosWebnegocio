package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.modulos.pagos.dominio.*;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.*;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ServicioEstadoPagoCheckoutTest {
    @org.junit.jupiter.api.Test void aprobacionDuplicadaNoReabreIncidenciaResuelta() {
        var orden=preparar(8L,"APROBADO","PAGADA_CON_INCIDENCIA");
        orden.setRequiereConciliacion(false);
        servicio.aplicar(8L,resultado(EstadoPago.APROBADO));
        assertThat(orden.isRequiereConciliacion()).isFalse();
        verifyNoInteractions(inventario,citas);
        verify(conciliacion).registrarCheckout(any(),any());
    }

    private final IntentoPagoCheckoutRepositorio intentos=mock(IntentoPagoCheckoutRepositorio.class);
    private final CheckoutOrdenRepositorio ordenes=mock(CheckoutOrdenRepositorio.class);
    private final ServicioInventarioCheckout inventario=mock(ServicioInventarioCheckout.class);
    private final ServicioConfirmacionCitasCheckout citas=mock(ServicioConfirmacionCitasCheckout.class);
    private final ServicioConciliacionPagos conciliacion=mock(ServicioConciliacionPagos.class);
    private final Clock reloj=Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"),ZoneOffset.UTC);
    private final ServicioEstadoPagoCheckout servicio=new ServicioEstadoPagoCheckout(intentos,ordenes,inventario,citas,conciliacion,mock(EntityManager.class),reloj);

    @Test void rechazoConservaApartadoEInventarioYPermiteOtroIntento() {
        var orden=preparar(8L,"CREADO","PAGO_EN_PROCESO");
        servicio.aplicar(8L,resultado(EstadoPago.RECHAZADO));
        assertThat(orden.getEstado()).isEqualTo("PENDIENTE_PAGO");
        verifyNoInteractions(inventario,citas);
        var segundo=intento(10L,"CREADO");
        when(intentos.findById(10L)).thenReturn(Optional.of(segundo));
        when(inventario.confirmarSiDisponible(7L,9L)).thenReturn(true);
        when(citas.confirmar(7L,9L)).thenReturn(true);
        servicio.aplicar(10L,resultado(EstadoPago.APROBADO));
        assertThat(orden.getEstado()).isEqualTo("PAGADA");
        verify(conciliacion).registrarCheckout(eq(segundo),any());
    }
    @Test void rechazoDespuesDelPlazoLibera() {
        var orden=preparar(8L,"CREADO","PAGO_EN_PROCESO");
        orden.setExpiraEn(LocalDateTime.now(reloj).minusSeconds(1));
        servicio.aplicar(8L,resultado(EstadoPago.RECHAZADO));
        assertThat(orden.getEstado()).isEqualTo("EXPIRADA");
        verify(inventario).expirar(7L,9L); verify(citas).liberar(7L,9L);
    }
    @Test void aprobadoTardioConHorarioOcupadoSeRegistraConIncidencia() {
        var orden=preparar(8L,"PROCESANDO","EXPIRADA");
        when(inventario.confirmarSiDisponible(7L,9L)).thenReturn(true);
        when(citas.confirmar(7L,9L)).thenReturn(false);
        var respuesta=servicio.aplicar(8L,resultado(EstadoPago.APROBADO));
        assertThat(respuesta.intento().getEstado()).isEqualTo("APROBADO");
        assertThat(orden.getEstado()).isEqualTo("PAGADA_CON_INCIDENCIA");
        assertThat(orden.isRequiereConciliacion()).isTrue();
        verify(intentos).save(respuesta.intento()); verify(ordenes).save(orden);
        verify(conciliacion).registrarCheckout(eq(respuesta.intento()),any());
        verify(citas).avisarIncidencia(7L,9L);
    }
    @Test void aprobadoTardioSinStockTambienSeRegistraSinConfirmarCitas() {
        var orden=preparar(8L,"PROCESANDO","EXPIRADA");
        when(inventario.confirmarSiDisponible(7L,9L)).thenReturn(false);
        servicio.aplicar(8L,resultado(EstadoPago.APROBADO));
        assertThat(orden.isRequiereConciliacion()).isTrue();
        verify(citas,never()).confirmar(any(),any());
        verify(conciliacion).registrarCheckout(any(),any());
    }
    @Test void eventoPendienteTardioNoDeshaceUnPagoAprobado() {
        var orden=preparar(8L,"APROBADO","PAGADA");
        servicio.aplicar(8L,resultado(EstadoPago.PROCESANDO));
        assertThat(orden.getEstado()).isEqualTo("PAGADA");
        verifyNoInteractions(citas,inventario,conciliacion); verify(intentos,never()).save(any());
    }
    @Test void cancelarLiberaAunqueQuedeTiempo() {
        var orden=preparar(8L,"PROCESANDO","PAGO_EN_PROCESO");
        servicio.aplicar(8L,resultado(EstadoPago.CANCELADO));
        assertThat(orden.getEstado()).isEqualTo("CANCELADA");
        verify(citas).liberar(7L,9L);verify(inventario).liberar(7L,9L);
    }
    @Test void procesamientoSinFechaRecibeTopeSinReiniciarloEnCadaEvento() {
        var orden=preparar(8L,"CREADO","PENDIENTE_PAGO");
        var limite=OffsetDateTime.now(reloj).plusHours(2);
        when(citas.prorrogar(7L,9L,null)).thenReturn(limite);
        when(citas.prorrogar(7L,9L,limite)).thenReturn(limite);
        servicio.aplicar(8L,resultado(EstadoPago.PROCESANDO));
        servicio.aplicar(8L,resultado(EstadoPago.PROCESANDO));
        assertThat(orden.getExpiraEn()).isEqualTo(limite.toLocalDateTime());
        verify(citas).prorrogar(7L,9L,null); verify(citas).prorrogar(7L,9L,limite);
    }
    private CheckoutOrdenEntidad preparar(Long id,String estadoIntento,String estadoOrden) {
        var intento=intento(id,estadoIntento);
        var orden=new CheckoutOrdenEntidad();orden.setId(9L);orden.setEmpresaId(7L);orden.setEstado(estadoOrden);
        orden.setExpiraEn(LocalDateTime.now(reloj).plusMinutes(15));
        when(intentos.findById(id)).thenReturn(Optional.of(intento));
        when(ordenes.bloquearPorEmpresaEId(7L,9L)).thenReturn(Optional.of(orden));return orden;
    }
    private IntentoPagoCheckoutEntidad intento(Long id,String estado) {
        var intento=new IntentoPagoCheckoutEntidad();intento.setId(id);intento.setEmpresaId(7L);
        intento.setCheckoutOrdenId(9L);intento.setEstado(estado);return intento;
    }
    private ResultadoCobro resultado(EstadoPago estado) {
        return new ResultadoCobro(ProveedorPago.MERCADO_PAGO,"orden","pago",estado,null,estado.name(),null,OffsetDateTime.now(reloj));
    }
}
