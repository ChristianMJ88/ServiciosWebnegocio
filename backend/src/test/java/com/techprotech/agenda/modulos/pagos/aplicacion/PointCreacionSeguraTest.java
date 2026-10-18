package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.modulos.caja.api.dto.*;
import com.techprotech.agenda.modulos.caja.aplicacion.ServicioVentasCaja;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.VentaEntidad;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.VentaRepositorio;
import com.techprotech.agenda.modulos.pagos.api.dto.CrearOrdenPointRequest;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago.*;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

class PointCreacionSeguraTest {
    final ClienteMercadoPagoPoint cliente=mock(ClienteMercadoPagoPoint.class);
    final ServicioVentasCaja ventas=mock(ServicioVentasCaja.class);
    final OrdenPointCajaRepositorio ordenes=mock(OrdenPointCajaRepositorio.class);
    final AtomicReference<OrdenPointCajaEntidad> local=new AtomicReference<>();
    final ServicioMercadoPagoPoint point;
    PointCreacionSeguraTest() {
        var terminales=mock(TerminalMercadoPagoRepositorio.class);var repos=mock(VentaRepositorio.class);var terminal=new TerminalMercadoPagoEntidad();
        terminal.setId(5L);terminal.setEmpresaId(7L);terminal.setSucursalId(3L);terminal.setTerminalId("TERMINAL_TEST");terminal.setActiva(true);
        when(terminales.bloquear(5L,7L)).thenReturn(Optional.of(terminal));when(terminales.findByIdAndEmpresaId(5L,7L)).thenReturn(Optional.of(terminal));
        var v=new VentaEntidad();v.setId(22L);v.setEmpresaId(7L);v.setFolio("V_TEST");when(repos.findByIdAndEmpresaId(22L,7L)).thenReturn(Optional.of(v));
        when(ventas.prepararPoint(any(),any(),any(),any())).thenReturn(new VentaResponse(22L,"V_TEST",3L,1L,4L,"PENDIENTE_PAGO","MXN",new BigDecimal("700"),BigDecimal.ZERO,BigDecimal.ZERO,new BigDecimal("700"),null,null,OffsetDateTime.parse("2026-10-17T12:00:00Z"),"CAJA",List.of()));
        when(ordenes.save(any())).thenAnswer(i->{local.set(i.getArgument(0));return i.getArgument(0);});
        when(ordenes.bloquear(eq(7L),anyString())).thenAnswer(i->Optional.ofNullable(local.get()));
        when(ordenes.findByEmpresaIdAndPublicId(eq(7L),anyString())).thenAnswer(i->Optional.ofNullable(local.get()));
        when(ventas.confirmarPoint(any(OrdenPointCajaEntidad.class),anyString(),anyList())).thenReturn(new ServicioVentasCaja.ConfirmacionPoint(null,false));
        var comisiones=mock(ServicioComisionPlataforma.class);when(comisiones.calcular(anyLong(),any(com.techprotech.agenda.modulos.pagos.dominio.ProveedorPago.class),any(),any())).thenReturn(BigDecimal.ZERO);
        point=new ServicioMercadoPagoPoint(cliente,mock(ServicioOAuthMercadoPago.class),new PropiedadesMercadoPago(true,"PRUEBA",null,null,null,null,null,null,null,null,null),terminales,ordenes,mock(SucursalRepositorio.class),repos,ventas,mock(ServicioConciliacionPagos.class),comisiones,ServicioMercadoPagoPointTest.gestor(),java.time.Clock.systemUTC(),java.time.Duration.ofMinutes(20),new PagosPrevistosPoint(new com.fasterxml.jackson.databind.ObjectMapper()));
    }
    @Test void falloDefinitivoCancelaLaReservaAntesDePropagar() {
        when(cliente.crearOrden(any(),any(),any(),any(),any(),any(),any())).thenThrow(new ErrorProveedorPoint(409));
        assertThatThrownBy(this::crear).hasMessageContaining("terminal ya tiene un cobro pendiente");
        assertThat(local.get().getEstado()).isEqualTo("CANCELADO");verify(ventas).cancelarPoint(eq(7L),eq(22L),any());
    }
    @Test void timeoutConservaCreandoYConsultaReutilizaIdempotencia() {
        when(cliente.crearOrden(any(),eq("CLAVE_TEST"),any(),any(),any(),any(),any()))
            .thenThrow(new org.springframework.web.client.ResourceAccessException("timeout"))
            .thenReturn(new ClienteMercadoPagoPoint.OrdenRemota("ORD_TEST","created",null,null,null,null));
        var respuesta=crear();assertThat(respuesta.estado()).isEqualTo("CREANDO");verify(ventas,never()).cancelarPoint(any(),any(),any());
        assertThat(point.reintentar(7L,List.of(),respuesta.id()).estado()).isEqualTo("CREADO");
        verify(cliente,times(2)).crearOrden(eq(7L),eq("CLAVE_TEST"),eq("TERMINAL_TEST"),eq("point_"+respuesta.id().replace("-","")),any(),any(),any());
        verify(ventas,times(1)).prepararPoint(any(),any(),any(),any());
    }
    @Test void erroresIndeterminadosYReenvio409NuncaLiberanSaldo() {
        when(cliente.crearOrden(any(),any(),any(),any(),any(),any(),any()))
            .thenThrow(new ErrorProveedorPoint(503)).thenThrow(new ErrorProveedorPoint(409)).thenThrow(new IllegalStateException("Mercado Pago no devolvió id"));
        var respuesta=crear();assertThat(respuesta.estado()).isEqualTo("CREANDO");
        assertThat(point.reintentar(7L,List.of(),respuesta.id()).estado()).isEqualTo("CREANDO");
        assertThat(point.reintentar(7L,List.of(),respuesta.id()).estado()).isEqualTo("CREANDO");
        verify(ventas,never()).cancelarPoint(any(),any(),any());
        local.get().setCreadoEn(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).minusHours(1));
        assertThat(point.consultar(7L,respuesta.id()).estado()).isEqualTo("REQUIERE_REVISION");
        verify(cliente,times(3)).crearOrden(any(),any(),any(),any(),any(),any(),any());
    }
    @Test void creaOrdenSoloPorRestoYConservaPartesSinRegistrarPagos() {
        when(cliente.crearOrden(any(),any(),any(),any(),any(),any(),any())).thenReturn(new ClienteMercadoPagoPoint.OrdenRemota("ORD_TEST","created",null,null,null,null));
        var pagos=List.of(new RegistrarVentaRequest.PagoVentaRequest("EFECTIVO",new BigDecimal("300"),new BigDecimal("500"),null),new RegistrarVentaRequest.PagoVentaRequest("MERCADO_PAGO_POINT",null,null,null));
        var respuesta=point.crearOrden(7L,11L,List.of(3L),"CLAVE_TEST",new CrearOrdenPointRequest(5L,new RegistrarVentaRequest(3L,1L,null,List.of(),null,null,null,null,null,pagos)));
        assertThat(respuesta.importe()).isEqualByComparingTo("400");assertThat(respuesta.efectivoPrevisto()).isEqualByComparingTo("300");assertThat(local.get().getPagosPrevistosJson()).contains("300.00");
        verify(cliente).crearOrden(eq(7L),any(),any(),any(),argThat(n -> n.compareTo(new BigDecimal("400"))==0),any(),any());verify(ventas,never()).confirmarPoint(any(OrdenPointCajaEntidad.class),anyString(),anyList());
    }
    @Test void getNoReenviaYNingunaAccionPermiteOtraSucursal() {
        when(cliente.crearOrden(any(),any(),any(),any(),any(),any(),any())).thenThrow(new ErrorProveedorPoint(503));
        var creada=crear();assertThat(point.consultar(7L,List.of(3L),creada.id()).estado()).isEqualTo("CREANDO");
        verify(cliente,times(1)).crearOrden(any(),any(),any(),any(),any(),any(),any());
        for(Runnable accion:List.<Runnable>of(()->point.consultar(7L,List.of(99L),creada.id()),()->point.cancelar(7L,List.of(99L),creada.id(),"cancel-test"),()->point.reembolsar(7L,List.of(99L),creada.id(),"refund-test"),()->point.simularAprobacion(7L,List.of(99L),creada.id())))
            assertThatThrownBy(accion::run).isInstanceOf(org.springframework.web.server.ResponseStatusException.class).hasMessageContaining("sucursal");
    }
    @Test void idempotenciaNoPermiteCambiarLaCuenta() {
        when(cliente.crearOrden(any(),any(),any(),any(),any(),any(),any())).thenThrow(new ErrorProveedorPoint(503));var creada=crear();
        when(ordenes.findByEmpresaIdAndIdempotencia(7L,"CLAVE_TEST")).thenReturn(Optional.of(local.get()));
        assertThatThrownBy(()->point.crearOrden(7L,11L,List.of(3L),"CLAVE_TEST",new CrearOrdenPointRequest(5L,new RegistrarVentaRequest(3L,99L,null,List.of(),"MERCADO_PAGO_POINT",null,null))))
            .hasMessageContaining("otra cuenta");verify(cliente,times(1)).crearOrden(any(),any(),any(),any(),any(),any(),any());
    }
    @Test void reembolsoExternoMixtoConservaVentaYEfectivoParaRevision() {
        when(cliente.crearOrden(any(),any(),any(),any(),any(),any(),any())).thenReturn(new ClienteMercadoPagoPoint.OrdenRemota("ORD_TEST","processed",null,"PAY_TEST","processed",null));
        var pagos=List.of(new RegistrarVentaRequest.PagoVentaRequest("EFECTIVO",new BigDecimal("300"),null,null),new RegistrarVentaRequest.PagoVentaRequest("MERCADO_PAGO_POINT",null,null,null));
        point.crearOrden(7L,11L,List.of(3L),"CLAVE_TEST",new CrearOrdenPointRequest(5L,new RegistrarVentaRequest(3L,1L,null,List.of(),null,null,null,null,null,pagos)));
        when(ordenes.findByProveedorOrdenId("ORD_TEST")).thenReturn(Optional.of(local.get()));when(cliente.consultar(7L,"ORD_TEST")).thenReturn(new ClienteMercadoPagoPoint.OrdenRemota("ORD_TEST","refunded",null,"PAY_TEST","refunded",null));
        assertThat(point.procesarWebhook("ORD_TEST")).isTrue();assertThat(local.get().getEstado()).isEqualTo("REEMBOLSADO");verify(ventas).revisarReembolsoPoint(7L,22L);verify(ventas,never()).marcarReembolsadaPoint(any());
    }
    private com.techprotech.agenda.modulos.pagos.api.dto.OrdenPointResponse crear() {
        return point.crearOrden(7L,11L,List.of(3L),"CLAVE_TEST",new CrearOrdenPointRequest(5L,new RegistrarVentaRequest(3L,1L,null,List.of(),"MERCADO_PAGO_POINT",null,null)));
    }
}
