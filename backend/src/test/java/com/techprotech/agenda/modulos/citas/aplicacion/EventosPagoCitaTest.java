package com.techprotech.agenda.modulos.citas.aplicacion;

import com.techprotech.agenda.modulos.caja.aplicacion.*;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.citas.dominio.eventos.*;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.productos.aplicacion.ServicioComisionesDevengadas;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventosPagoCitaTest {
    @Mock CitaRepositorio citas;
    @Mock ApplicationEventPublisher bus;
    @Mock VentaRepositorio ventas;
    @Mock VentaDetalleRepositorio detalles;
    @Mock PagoVentaRepositorio pagos;
    @Mock ServicioComisionesDevengadas comisiones;
    @Mock ServicioSaldoCitas saldos;
    @InjectMocks ServicioVentasCaja caja;
    final Clock reloj = Clock.fixed(Instant.parse("2026-10-16T18:00:00Z"), ZoneOffset.UTC);
    PublicadorPagosCitas eventos;

    @BeforeEach void preparar() {
        eventos = new PublicadorPagosCitas(citas, new PublicadorEventosCita(bus, reloj));
        ReflectionTestUtils.setField(caja, "eventosPago", eventos);
        ReflectionTestUtils.setField(caja, "reloj", reloj);
    }

    @ParameterizedTest @EnumSource(value=OrigenEventoCita.class,names={"ADMIN","WEBHOOK"})
    void ventaMixtaPublicaSoloLoAcreditadoACadaCitaYConsultaElTenantUnaVez(OrigenEventoCita origen) {
        var a = cita(1L); var b = cita(2L); var venta = venta(); var pago = pago();
        var filas = List.of(detalle(1L,"SERVICIO","80"), detalle(1L,"SERVICIO","20"),
                detalle(2L,"SERVICIO","150"), detalle(null,"PRODUCTO","500"), detalle(null,"ENVIO","100"));
        when(citas.findByEmpresaIdAndIdIn(eq(7L),any())).thenReturn(List.of(a,b));
        eventos.venta(venta,pago,filas,origen,origen==OrigenEventoCita.ADMIN?11L:null);
        var captor = ArgumentCaptor.forClass(PagoCitaRegistrado.class); verify(bus,times(2)).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).extracting(PagoCitaRegistrado::monto)
                .containsExactly(new BigDecimal("100"),new BigDecimal("150"));
        assertThat(captor.getAllValues()).allSatisfy(e -> {
            assertThat(e.datos().empresaId()).isEqualTo(7L); assertThat(e.datos().sucursalId()).isEqualTo(3L);
            assertThat(e.datos().origen()).isEqualTo(origen); assertThat(e.datos().momento()).isEqualTo(reloj.instant());
            assertThat(e.ventaId()).isEqualTo(30L); assertThat(e.pagoId()).isEqualTo(40L);
            assertThat(e.tipoRegistro()).isEqualTo(PagoCitaRegistrado.TipoRegistro.PAGO_VENTA);
        });
        verify(citas).findByEmpresaIdAndIdIn(eq(7L),any()); verifyNoMoreInteractions(citas,bus);
    }

    @Test void pagoDirectoUsaElImportePersistidoYSnapshotSinOtraConsulta() {
        var pago = new PagoCitaEntidad(); pago.setId(99L); pago.setEmpresaId(7L); pago.setCitaId(1L);
        pago.setMonto(new BigDecimal("35.00")); pago.setRegistradoPorUsuarioId(11L);
        eventos.pagoDirecto(cita(1L),pago);
        verify(bus).publishEvent(argThat((Object e) -> e instanceof PagoCitaRegistrado p
                && p.pagoId().equals(99L) && p.monto().compareTo(new BigDecimal("35"))==0
                && p.tipoRegistro()==PagoCitaRegistrado.TipoRegistro.PAGO_CITA && p.ventaId()==null));
        verifyNoInteractions(citas);
    }

    @Test void puntoVirtualAprobadoPublicaUnaVezYAunqueElWebhookSeRepitaNoDuplica() {
        var venta = venta(); venta.setEstado("PENDIENTE_PAGO");
        var filas = List.of(detalle(1L,"SERVICIO","80"),detalle(null,"PRODUCTO","500"));
        var guardados = new ArrayList<PagoVentaEntidad>();
        when(ventas.findConBloqueoByIdAndEmpresaId(30L,7L)).thenReturn(Optional.of(venta));
        when(detalles.findByEmpresaIdAndVentaIdOrderByIdAsc(7L,30L)).thenReturn(filas);
        when(pagos.findByEmpresaIdAndVentaIdOrderByRegistradoEnDesc(7L,30L)).thenAnswer(i -> List.copyOf(guardados));
        when(pagos.save(any())).thenAnswer(i -> { var p=(PagoVentaEntidad)i.getArgument(0); p.setId(40L); guardados.add(p); return p; });
        when(citas.findByEmpresaIdAndIdIn(eq(7L),any())).thenReturn(List.of(cita(1L)));
        when(citas.bloquearPorIdYEmpresa(1L,7L)).thenReturn(Optional.of(cita(1L)));
        when(saldos.calcular(eq(7L),any())).thenReturn(Map.of(1L,new SaldoCita(new BigDecimal("80"),BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,new BigDecimal("80"),EstadoCobroCita.PENDIENTE)));
        caja.confirmarPoint(7L,30L,"point-prueba"); caja.confirmarPoint(7L,30L,"point-prueba");
        verify(bus).publishEvent(argThat((Object e) -> e instanceof PagoCitaRegistrado p
                && p.datos().origen()==OrigenEventoCita.WEBHOOK && p.monto().compareTo(new BigDecimal("80"))==0));
        verifyNoMoreInteractions(bus); verify(pagos).save(any());
    }

    @Test void productosSinCitasNoEmitenPagoCitaNiConsultanLaAgenda() {
        eventos.venta(venta(),pago(),List.of(detalle(null,"PRODUCTO","500")),OrigenEventoCita.WEBHOOK,null);
        verifyNoInteractions(bus,citas);
    }

    @ParameterizedTest @ValueSource(strings={"PAGADA","PAGADA_CON_INCIDENCIA"})
    void proyeccionOnlineIncluidaIncidenciaPublicaAnticipoUnaVezSinImporteDeProductos(String estado) {
        var ordenes = mock(com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenRepositorio.class);
        var origen = mock(com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenDetalleRepositorio.class);
        var intentos = mock(com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.IntentoPagoCheckoutRepositorio.class);
        var orden = new com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenEntidad();
        orden.setId(50L); orden.setEmpresaId(7L); orden.setSucursalId(3L); orden.setEstado(estado);
        orden.setMoneda("MXN"); orden.setPublicId("prueba-006"); orden.setTotal(new BigDecimal("580"));
        var intento = new com.techprotech.agenda.modulos.pagos.infraestructura.entidad.IntentoPagoCheckoutEntidad();
        intento.setId(60L); intento.setImporte(orden.getTotal());
        var servicio = new com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenDetalleEntidad();
        servicio.setTipoConcepto("SERVICIO"); servicio.setCitaId(1L); servicio.setTotal(new BigDecimal("80"));
        var producto = new com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenDetalleEntidad();
        producto.setTipoConcepto("PRODUCTO"); producto.setTotal(new BigDecimal("500"));
        when(ordenes.bloquearPorEmpresaEId(7L,50L)).thenReturn(Optional.of(orden));
        when(ventas.existsByEmpresaIdAndCheckoutOrdenId(7L,50L)).thenReturn(false,true);
        when(intentos.findFirstByEmpresaIdAndCheckoutOrdenIdAndEstadoOrderByIdDesc(7L,50L,"APROBADO"))
                .thenReturn(Optional.of(intento));
        when(origen.findByEmpresaIdAndCheckoutOrdenIdOrderByIdAsc(7L,50L)).thenReturn(List.of(servicio,producto));
        when(ventas.save(any())).thenAnswer(i -> { var v=(VentaEntidad)i.getArgument(0); v.setId(30L); return v; });
        when(detalles.saveAll(any())).thenAnswer(i -> i.getArgument(0));
        when(pagos.save(any())).thenAnswer(i -> { var p=(PagoVentaEntidad)i.getArgument(0); p.setId(40L); return p; });
        when(citas.findByEmpresaIdAndIdIn(eq(7L),any())).thenReturn(List.of(cita(1L)));
        var proyector = new ServicioVentasCheckout(ordenes,origen,intentos,ventas,detalles,pagos,comisiones,reloj,eventos);
        proyector.proyectar(7L,50L); proyector.proyectar(7L,50L);
        verify(bus).publishEvent(argThat((Object e) -> e instanceof PagoCitaRegistrado p
                && p.checkoutOrdenId().equals(50L) && p.monto().compareTo(new BigDecimal("80"))==0
                && p.datos().origen()==OrigenEventoCita.WEBHOOK && p.datos().actorId()==null));
        verifyNoMoreInteractions(bus); verify(pagos).save(any());
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void noSePublicaInformacionDeOtroTenantOSucursal(boolean sucursalAjena) {
        var ajena = cita(1L); if(sucursalAjena) ajena.setSucursalId(8L); else ajena.setEmpresaId(8L);
        when(citas.findByEmpresaIdAndIdIn(eq(7L),any())).thenReturn(List.of(ajena));
        assertThatThrownBy(() -> eventos.venta(venta(),pago(),List.of(detalle(1L,"SERVICIO","80")),OrigenEventoCita.WEBHOOK,null))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(bus);
    }

    static CitaEntidad cita(Long id) { var c=CierreCitaUnificadoTest.cita(); c.setId(id); c.setMoneda("MXN"); return c; }
    static VentaEntidad venta() {
        var v = new VentaEntidad(); v.setId(30L); v.setEmpresaId(7L); v.setSucursalId(3L); v.setMoneda("MXN");
        v.setEstado("PAGADA"); v.setTotal(new BigDecimal("580")); return v;
    }
    static PagoVentaEntidad pago() { var p=new PagoVentaEntidad(); p.setId(40L); p.setMonto(new BigDecimal("850")); return p; }
    static VentaDetalleEntidad detalle(Long citaId,String tipo,String monto) {
        var d=new VentaDetalleEntidad(); d.setCitaId(citaId); d.setTipoConcepto(tipo); d.setTotal(new BigDecimal(monto)); return d;
    }
}
