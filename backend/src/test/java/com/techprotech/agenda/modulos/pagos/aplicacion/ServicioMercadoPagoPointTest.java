package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.modulos.caja.api.dto.RegistrarVentaRequest;
import com.techprotech.agenda.modulos.caja.api.dto.VentaResponse;
import com.techprotech.agenda.modulos.caja.aplicacion.ServicioVentasCaja;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.VentaEntidad;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.VentaRepositorio;
import com.techprotech.agenda.modulos.pagos.api.dto.CrearOrdenPointRequest;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.OrdenPointCajaEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.TerminalMercadoPagoEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago.ClienteMercadoPagoPoint;
import com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago.PropiedadesMercadoPago;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.OrdenPointCajaRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.TerminalMercadoPagoRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ServicioMercadoPagoPointTest {
    @Test
    void publicaLaTerminalVirtualConfiguradaEnAmbienteDePrueba() {
        ClienteMercadoPagoPoint cliente = mock(ClienteMercadoPagoPoint.class);
        ServicioOAuthMercadoPago oauth = mock(ServicioOAuthMercadoPago.class);
        PropiedadesMercadoPago propiedades = propiedades("POINT_VIRTUAL_CONFIGURADA");
        TerminalMercadoPagoRepositorio terminales = mock(TerminalMercadoPagoRepositorio.class);
        OrdenPointCajaRepositorio ordenes = mock(OrdenPointCajaRepositorio.class);
        SucursalRepositorio sucursales = mock(SucursalRepositorio.class);
        VentaRepositorio ventasRepositorio = mock(VentaRepositorio.class);
        ServicioVentasCaja ventas = mock(ServicioVentasCaja.class);
        ServicioMercadoPagoPoint servicio = new ServicioMercadoPagoPoint(cliente, oauth, propiedades, terminales, ordenes,
                sucursales, ventasRepositorio, ventas);
        when(oauth.esAmbientePrueba()).thenReturn(true);
        when(cliente.listarTerminales(7L)).thenReturn(List.of());
        when(terminales.findByEmpresaIdOrderByNombreAsc(7L)).thenReturn(List.of());
        when(sucursales.findByEmpresaIdOrderByNombreAsc(7L)).thenReturn(List.of());

        var terminalesDisponibles = servicio.listarTerminales(7L);

        assertThat(terminalesDisponibles).singleElement().satisfies(terminal -> {
            assertThat(terminal.terminalId()).isEqualTo("POINT_VIRTUAL_CONFIGURADA");
            assertThat(terminal.modoOperacion()).isEqualTo("PDV");
            assertThat(terminal.configurada()).isFalse();
        });
    }

    @Test
    void confirmaLaVentaCuandoPointDevuelvePagoProcesado() {
        ClienteMercadoPagoPoint cliente = mock(ClienteMercadoPagoPoint.class);
        ServicioOAuthMercadoPago oauth = mock(ServicioOAuthMercadoPago.class);
        PropiedadesMercadoPago propiedades = propiedades("POINT_VIRTUAL_CONFIGURADA");
        TerminalMercadoPagoRepositorio terminales = mock(TerminalMercadoPagoRepositorio.class);
        OrdenPointCajaRepositorio ordenes = mock(OrdenPointCajaRepositorio.class);
        SucursalRepositorio sucursales = mock(SucursalRepositorio.class);
        VentaRepositorio ventasRepositorio = mock(VentaRepositorio.class);
        ServicioVentasCaja ventas = mock(ServicioVentasCaja.class);
        ServicioMercadoPagoPoint servicio = new ServicioMercadoPagoPoint(cliente, oauth, propiedades, terminales, ordenes,
                sucursales, ventasRepositorio, ventas);

        TerminalMercadoPagoEntidad terminal = new TerminalMercadoPagoEntidad();
        terminal.setId(5L); terminal.setEmpresaId(7L); terminal.setSucursalId(3L); terminal.setTerminalId("POINT__123");
        terminal.setNombre("Caja principal"); terminal.setActiva(true);
        when(terminales.findByIdAndEmpresaId(5L, 7L)).thenReturn(Optional.of(terminal));
        when(ordenes.findByEmpresaIdAndIdempotencia(7L, "key-1")).thenReturn(Optional.empty());
        RegistrarVentaRequest ventaRequest = new RegistrarVentaRequest(3L, null, null, List.of(), "MERCADO_PAGO_POINT", null, null);
        VentaResponse venta = new VentaResponse(22L, "V-22", 3L, null, null, "PENDIENTE_PAGO", "MXN",
                new BigDecimal("180.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("180.00"), null, null,
                LocalDateTime.now(), "CAJA", List.of());
        when(ventas.prepararPoint(eq(7L), eq(9L), eq(List.of(3L)), any())).thenReturn(venta);
        when(cliente.crearOrden(eq(7L), eq("key-1"), eq("POINT__123"), anyString(), eq(new BigDecimal("180.00")), anyString()))
                .thenReturn(new ClienteMercadoPagoPoint.OrdenRemota("ORD-1", "processed", "accredited", "PAY-1", "processed", "accredited"));
        when(ordenes.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        VentaEntidad ventaEntidad = new VentaEntidad(); ventaEntidad.setId(22L); ventaEntidad.setEmpresaId(7L); ventaEntidad.setFolio("V-22");
        when(ventasRepositorio.findByIdAndEmpresaId(22L, 7L)).thenReturn(Optional.of(ventaEntidad));

        var respuesta = servicio.crearOrden(7L, 9L, List.of(3L), "key-1", new CrearOrdenPointRequest(5L, ventaRequest));

        assertThat(respuesta.estado()).isEqualTo("APROBADO");
        assertThat(respuesta.proveedorOrdenId()).isEqualTo("ORD-1");
        verify(ventas).confirmarPoint(7L, 22L, "PAY-1");
    }

    private PropiedadesMercadoPago propiedades(String terminalPruebaId) {
        return new PropiedadesMercadoPago(true, "PRUEBA", "", "", "", "", "", "", "", "", terminalPruebaId);
    }
}
