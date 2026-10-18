package com.techprotech.agenda.modulos.caja.aplicacion;

import com.techprotech.agenda.modulos.caja.api.dto.RegistrarVentaRequest;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.productos.aplicacion.ServicioInventarioProductos;
import com.techprotech.agenda.modulos.productos.infraestructura.entidad.ProductoEntidad;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.ProductoExistenciaRepositorio;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.ProductoRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@org.junit.jupiter.api.parallel.Isolated("JVM México para escritura UTC de venta/pago")
class ServicioVentasCajaTest {
    private java.util.TimeZone zonaAnterior;
    @org.junit.jupiter.api.BeforeEach void zonaMexico() {
        zonaAnterior=java.util.TimeZone.getDefault();
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/Mexico_City"));
    }
    @org.junit.jupiter.api.AfterEach void restaurarZona() { java.util.TimeZone.setDefault(zonaAnterior); }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"false,false","true,false","false,true","true,true"})
    void registraServicioYProductosComoUnaVentaYDescuentaInventario(boolean regional, boolean promocion) {
        VentaRepositorio ventas = mock(VentaRepositorio.class);
        VentaDetalleRepositorio detalles = mock(VentaDetalleRepositorio.class);
        PagoVentaRepositorio pagos = mock(PagoVentaRepositorio.class);
        CajaSesionRepositorio sesiones = mock(CajaSesionRepositorio.class);
        MovimientoCajaRepositorio movimientos = mock(MovimientoCajaRepositorio.class);
        PagoCitaRepositorio pagosCita = mock(PagoCitaRepositorio.class);
        CitaRepositorio citas = mock(CitaRepositorio.class);
        ClienteRepositorio clientes = mock(ClienteRepositorio.class);
        ServicioRepositorio servicios = mock(ServicioRepositorio.class);
        ProductoRepositorio productos = mock(ProductoRepositorio.class);
        ProductoExistenciaRepositorio existencias = mock(ProductoExistenciaRepositorio.class);
        SucursalRepositorio sucursales = mock(SucursalRepositorio.class);
        ServicioInventarioProductos inventario = mock(ServicioInventarioProductos.class);
        var descuentos = mock(com.techprotech.agenda.modulos.productos.infraestructura.repositorio.ReglaDescuentoProductoRepositorio.class);
        var zonas = mock(com.techprotech.agenda.modulos.productos.infraestructura.repositorio.ZonaPrecioProductoRepositorio.class);
        var preciosZona = mock(com.techprotech.agenda.modulos.productos.infraestructura.repositorio.PrecioZonaProductoRepositorio.class);
        var bus=mock(org.springframework.context.ApplicationEventPublisher.class);
        ServicioVentasCaja servicio = new ServicioVentasCaja(ventas, detalles, pagos, sesiones, movimientos, pagosCita, citas, clientes, servicios, productos, existencias, sucursales, inventario, descuentos, zonas, preciosZona, mock(com.techprotech.agenda.modulos.productos.aplicacion.ServicioComisionesDevengadas.class), java.time.Clock.fixed(java.time.Instant.parse("2026-10-15T01:00:00Z"),java.time.ZoneOffset.UTC), com.techprotech.agenda.compartido.tiempo.TiempoPruebas.zona(java.time.ZoneId.of("America/Mexico_City")), new com.techprotech.agenda.modulos.citas.aplicacion.PublicadorPagosCitas(citas, new com.techprotech.agenda.modulos.citas.aplicacion.PublicadorEventosCita(bus, java.time.Clock.systemUTC())), new ServicioSaldoCitas(citas,pagosCita,detalles));

        SucursalEntidad sucursal = new SucursalEntidad(); sucursal.setId(5L); sucursal.setEmpresaId(1L); sucursal.setActiva(true);
        CajaSesionEntidad sesion = new CajaSesionEntidad(); sesion.setId(8L); sesion.setEmpresaId(1L); sesion.setSucursalId(5L); sesion.setEstado("ABIERTA");
        CitaEntidad cita = new CitaEntidad(); cita.setId(10L); cita.setEmpresaId(1L); cita.setSucursalId(5L); cita.setServicioId(20L);
        cita.setClienteId(30L); cita.setPrecio(new BigDecimal("100.00")); cita.setMoneda("MXN"); cita.setEstado("FINALIZADA"); cita.setCheckInEn(LocalDateTime.now());
        cita.setPrecioLista(new BigDecimal(promocion?"125.00":"100.00"));cita.setDescuento(new BigDecimal(promocion?"25.00":"0.00"));
        ServicioEntidad conceptoServicio = new ServicioEntidad(); conceptoServicio.setId(20L); conceptoServicio.setEmpresaId(1L); conceptoServicio.setNombre("Manicure");
        ProductoEntidad producto = new ProductoEntidad(); producto.setId(40L); producto.setEmpresaId(1L); producto.setActivo(true);
        producto.setControlaInventario(true); producto.setNombre("Crema"); producto.setSku("CREMA-1"); producto.setPrecioBase(new BigDecimal("50.00")); producto.setMoneda("MXN"); producto.setCosto(new BigDecimal("20.00"));
        var regla = new com.techprotech.agenda.modulos.productos.infraestructura.entidad.ReglaDescuentoProductoEntidad();
        regla.setActiva(true); regla.setNombre("Descuento producto"); regla.setAlcance("PRODUCTO"); regla.setProductoId(40L);
        regla.setTipoDescuento("PORCENTAJE"); regla.setValor(new BigDecimal("10")); regla.setCantidadMinima(BigDecimal.ONE);
        when(descuentos.findByEmpresaIdOrderByPrioridadDescNombreAsc(1L)).thenReturn(List.of(regla));
        if(regional) {
            sucursal.setZonaPrecioId(70L);
            var zona=new com.techprotech.agenda.modulos.productos.infraestructura.entidad.ZonaPrecioProductoEntidad(); zona.setId(70L);zona.setActiva(true);
            var precio=new com.techprotech.agenda.modulos.productos.infraestructura.entidad.PrecioZonaProductoEntidad();precio.setProductoId(40L);precio.setZonaPrecioId(70L);precio.setActivo(true);precio.setPrecio(new BigDecimal("60.00"));precio.setMoneda("MXN");
            when(zonas.findByIdAndEmpresaId(70L,1L)).thenReturn(Optional.of(zona));
            when(preciosZona.findByEmpresaIdAndProductoIdIn(eq(1L),any())).thenReturn(List.of(precio));
        }

        when(sucursales.findByIdAndEmpresaIdAndActivaTrue(5L, 1L)).thenReturn(Optional.of(sucursal));
        when(sesiones.findByEmpresaIdAndSucursalIdAndEstado(1L, 5L, "ABIERTA")).thenReturn(Optional.of(sesion));
        when(sesiones.findParaAperturaConBloqueo(1L,5L,"ABIERTA")).thenReturn(Optional.of(sesion));
        when(productos.findAllById(any())).thenReturn(List.of(producto));
        when(productos.findByEmpresaIdAndIdIn(eq(1L), any())).thenReturn(List.of(producto));
        when(citas.bloquearPorIdYEmpresa(10L, 1L)).thenReturn(Optional.of(cita));
        when(servicios.findByIdAndEmpresaId(20L, 1L)).thenReturn(Optional.of(conceptoServicio));
        when(pagosCita.sumarPagadoPorCita(1L, 10L)).thenReturn(BigDecimal.ZERO);
        when(detalles.sumarServiciosPagadosPorCita(1L, 10L)).thenReturn(BigDecimal.ZERO);
        when(ventas.save(any())).thenAnswer(invocation -> { VentaEntidad item = invocation.getArgument(0); item.setId(50L); return item; });
        when(detalles.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(pagos.save(any())).thenAnswer(invocation -> { PagoVentaEntidad item = invocation.getArgument(0); item.setId(60L); return item; });

        var cotizacion = servicio.cotizarProductos(1L, List.of(5L), new com.techprotech.agenda.modulos.caja.api.dto.CotizacionCajaRequest(5L,
                List.of(new RegistrarVentaRequest.ProductoVentaRequest(40L, new BigDecimal("2")))));
        assertThat(cotizacion.total()).isEqualByComparingTo(regional?"108.00":"90.00");
        assertThat(cotizacion.descuento()).isEqualByComparingTo(regional?"12.00":"10.00");
        var response = servicio.registrar(1L, 2L, List.of(5L), new RegistrarVentaRequest(5L, 10L, null,
                List.of(new RegistrarVentaRequest.ProductoVentaRequest(40L, new BigDecimal("2"))), "TARJETA", "AUTH-1", null));

        assertThat(response.total()).isEqualByComparingTo(regional?"208.00":"190.00");
        assertThat(response.descuentoTotal()).isEqualByComparingTo(new BigDecimal(regional?"12.00":"10.00").add(new BigDecimal(promocion?"25.00":"0.00")));
        assertThat(response.detalles().get(0).descuento()).isEqualByComparingTo(promocion?"25.00":"0.00");
        assertThat(response.detalles().get(1).descuento()).isEqualByComparingTo(regional?"12.00":"10.00");
        assertThat(response.creadaEn()).isEqualTo(java.time.OffsetDateTime.parse("2026-10-15T01:00Z"));
        assertThat(response.folio()).startsWith("V-261014190000-");
        var pagoCapturado=org.mockito.ArgumentCaptor.forClass(PagoVentaEntidad.class);
        verify(pagos).save(pagoCapturado.capture());
        assertThat(pagoCapturado.getValue().getRegistradoEn()).isEqualTo(response.creadaEn().toLocalDateTime());
        assertThat(response.detalles()).extracting(item -> item.tipoConcepto()).containsExactly("SERVICIO", "PRODUCTO");
        assertThat(response.detalles().get(1).costoUnitarioHistorico()).isEqualByComparingTo("20.00");
        assertThat(response.detalles().get(0).costoUnitarioHistorico()).isNull();
        producto.setCosto(new BigDecimal("30.00"));
        assertThat(response.detalles().get(1).costoUnitarioHistorico()).isEqualByComparingTo("20.00");
        verify(inventario).descontarVenta(eq(1L), eq(2L), eq(List.of(5L)), eq(5L), eq(java.util.Map.of(40L, new BigDecimal("2"))), any());
        verify(movimientos, never()).save(any());
        var cliente=new com.techprotech.agenda.modulos.clientes.infraestructura.entidad.ClienteEntidad();cliente.setUsuarioId(30L);
        when(clientes.findByClienteIdAndEmpresaId(30L,1L)).thenReturn(Optional.of(cliente));
        clearInvocations(inventario,pagos,sesiones,movimientos,detalles);
        var pendiente=servicio.registrarStaff(1L,77L,5L,30L,List.of(new RegistrarVentaRequest.ProductoVentaRequest(40L,new BigDecimal("2"))));
        assertThat(pendiente.estado()).isEqualTo("POR_COBRAR");
        assertThat(pendiente.canal()).isEqualTo("STAFF");
        assertThat(pendiente.total()).isEqualByComparingTo(regional?"108.00":"90.00");
        verifyNoInteractions(inventario,pagos,sesiones,movimientos);
        var captor=org.mockito.ArgumentCaptor.forClass(java.util.List.class);
        verify(detalles).saveAll(captor.capture());
        assertThat(((VentaDetalleEntidad)captor.getValue().get(0)).getPrestadorResponsableId()).isEqualTo(77L);
        var ventaCaptor=org.mockito.ArgumentCaptor.forClass(VentaEntidad.class);
        verify(ventas,times(2)).save(ventaCaptor.capture());
        var cuenta=ventaCaptor.getValue();
        when(ventas.findConBloqueoByIdAndEmpresaId(50L,1L)).thenReturn(Optional.of(cuenta));
        when(detalles.findByEmpresaIdAndVentaIdOrderByIdAsc(1L,50L)).thenReturn(captor.getValue());
        var paraPoint=servicio.prepararPoint(1L,2L,List.of(5L),new RegistrarVentaRequest(5L,null,null,List.of(),"MERCADO_PAGO_POINT",null,null,50L));
        assertThat(paraPoint.estado()).isEqualTo("PENDIENTE_PAGO");
        verifyNoInteractions(pagos);
        servicio.cancelarPoint(1L,50L,"Prueba cancelación terminal");
        assertThat(cuenta.getEstado()).isEqualTo("POR_COBRAR");
        verify(inventario).reintegrarVenta(eq(1L),eq(77L),eq(5L),any(),any(),any());
        clearInvocations(inventario);
        var cobrada=servicio.cobrarPendiente(1L,2L,List.of(5L),50L,"TARJETA","AUTH-STAFF");
        assertThat(cobrada.id()).isEqualTo(pendiente.id());
        assertThat(cobrada.estado()).isEqualTo("PAGADA");
        assertThat(cobrada.total()).isEqualByComparingTo(pendiente.total());
        assertThat(cuenta.getCreadaPorUsuarioId()).isEqualTo(77L);
        verify(pagos,times(1)).save(any());
        verify(inventario,times(1)).descontarVenta(eq(1L),eq(2L),eq(List.of(5L)),eq(5L),any(),any());
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.web.server.ResponseStatusException.class,
            ()->servicio.cobrarPendiente(1L,2L,List.of(5L),50L,"TARJETA",null));
        verify(pagos,times(1)).save(any());


        verify(bus).publishEvent(argThat((Object e) -> e instanceof com.techprotech.agenda.modulos.citas.dominio.eventos.PagoCitaRegistrado p
                && p.monto().compareTo(new BigDecimal("100"))==0 && p.datos().citaId().equals(10L)));
        verifyNoMoreInteractions(bus);
    }
}
