package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.compartido.direcciones.aplicacion.ServicioValidacionDirecciones;
import com.techprotech.agenda.compartido.direcciones.dominio.Direccion;
import com.techprotech.agenda.compartido.direcciones.dominio.ResultadoValidacionDireccion;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.PagoCitaRepositorio;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.VentaDetalleRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.pagos.api.dto.*;
import com.techprotech.agenda.modulos.pagos.infraestructura.checkout.PropiedadesCheckout;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenDetalleRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenRepositorio;
import com.techprotech.agenda.modulos.productos.aplicacion.ServicioCotizacionProductos;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioConfigEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ServicioOrdenCheckoutTest {
    private ServicioConfiguracionCheckout configuracion;
    private ServicioCotizacionProductos cotizador;
    private CitaRepositorio citas;
    private UsuarioRepositorio usuarios;
    private CheckoutOrdenRepositorio ordenes;
    private CheckoutOrdenDetalleRepositorio detalles;
    private ServicioOrdenCheckout servicio;

    @BeforeEach
    void preparar() {
        configuracion = mock(ServicioConfiguracionCheckout.class);
        EmpresaSitioConfigRepositorio sitios = mock(EmpresaSitioConfigRepositorio.class);
        SucursalRepositorio sucursales = mock(SucursalRepositorio.class);
        cotizador = mock(ServicioCotizacionProductos.class);
        citas = mock(CitaRepositorio.class);
        ServicioRepositorio servicios = mock(ServicioRepositorio.class);
        usuarios = mock(UsuarioRepositorio.class);
        PagoCitaRepositorio pagosCita = mock(PagoCitaRepositorio.class);
        VentaDetalleRepositorio detallesVenta = mock(VentaDetalleRepositorio.class);
        ordenes = mock(CheckoutOrdenRepositorio.class);
        detalles = mock(CheckoutOrdenDetalleRepositorio.class);
        EmpresaSitioConfigEntidad sitio = new EmpresaSitioConfigEntidad();
        sitio.setEmpresaId(25L); sitio.setSlug("nail-art"); sitio.setPublicado(true);
        when(sitios.findBySlug("nail-art")).thenReturn(Optional.of(sitio));
        SucursalEntidad sucursal = new SucursalEntidad(); sucursal.setId(3L); sucursal.setEmpresaId(25L); sucursal.setActiva(true);
        when(sucursales.findByIdAndEmpresaIdAndActivaTrue(3L, 25L)).thenReturn(Optional.of(sucursal));
        when(configuracion.obtenerPublica("nail-art")).thenReturn(configHabilitada());
        when(ordenes.save(any())).thenAnswer(invocacion -> {
            CheckoutOrdenEntidad orden = invocacion.getArgument(0); orden.setId(44L); return orden;
        });
        when(detalles.findByEmpresaIdAndCheckoutOrdenIdOrderByIdAsc(any(), any())).thenReturn(List.of());
        ServicioValidacionDirecciones direcciones = mock(ServicioValidacionDirecciones.class);
        when(direcciones.validarParaPersistencia(any())).thenAnswer(invocacion -> {
            Direccion direccion = invocacion.getArgument(0);
            return new ResultadoValidacionDireccion(true, "VALIDADA", "Dirección verificada",
                    "Reforma 123, Centro, Puebla", direccion, "place-1", new BigDecimal("19.0433"), new BigDecimal("-98.2019"));
        });
        servicio = new ServicioOrdenCheckout(configuracion, sitios, sucursales, cotizador, citas, servicios,
                usuarios, pagosCita, detallesVenta, ordenes, detalles, new PropiedadesCheckout(20), direcciones);
    }

    @Test
    void creaOrdenConPrecioCalculadoPorElBackend() {
        when(cotizador.cotizar(eq(25L), eq(3L), eq("CENTRO"), any())).thenReturn(
                new ServicioCotizacionProductos.ResultadoCotizacion(8L, List.of(
                        new ServicioCotizacionProductos.CotizacionProducto(12L, "Aceite", new BigDecimal("2"),
                                new BigDecimal("100.00"), new BigDecimal("80.00"), new BigDecimal("40.00"),
                                new BigDecimal("160.00"), "MXN"))));
        CrearOrdenCheckoutRequest request = new CrearOrdenCheckoutRequest(3L, "CENTRO", "cliente@test.mx",
                "Ana Pérez", "2221234567", "RECOGE_EN_SUCURSAL", null,
                List.of(new CrearOrdenCheckoutRequest.ProductoOrdenRequest(12L, new BigDecimal("2"))), List.of());

        OrdenCheckoutResponse respuesta = servicio.crear("nail-art", "carrito-123", request);

        assertThat(respuesta.total()).isEqualByComparingTo("160.00");
        assertThat(respuesta.descuentoTotal()).isEqualByComparingTo("40.00");
        assertThat(respuesta.estado()).isEqualTo("PENDIENTE_PAGO");
        verify(ordenes).save(argThat(orden -> orden.getEmpresaId().equals(25L)
                && orden.getIdempotencia().equals("carrito-123") && orden.getZonaPrecioId().equals(8L)));
    }

    @Test
    void agregaEntregaLocalYConservaLaDireccionEnLaOrden() {
        when(configuracion.obtenerPublica("nail-art")).thenReturn(new ConfiguracionCheckoutPublicaResponse(
                true, "MERCADO_PAGO", "PRUEBA", "public-key", true, false, false, true, true,
                true, true, new BigDecimal("49.00"), true, new BigDecimal("129.00"),
                "PAGO_COMPLETO", null, null, "MXN"));
        when(cotizador.cotizar(eq(25L), eq(3L), isNull(), any())).thenReturn(
                new ServicioCotizacionProductos.ResultadoCotizacion(null, List.of(
                        new ServicioCotizacionProductos.CotizacionProducto(12L, "Aceite", BigDecimal.ONE,
                                new BigDecimal("160.00"), new BigDecimal("160.00"), BigDecimal.ZERO,
                                new BigDecimal("160.00"), "MXN"))));
        CrearOrdenCheckoutRequest.DireccionEntregaRequest direccion =
                new CrearOrdenCheckoutRequest.DireccionEntregaRequest("Reforma", "123", "4",
                        "Centro", "Puebla", "Puebla", "72000", "MX", "Portón blanco");
        CrearOrdenCheckoutRequest request = new CrearOrdenCheckoutRequest(3L, null, "cliente@test.mx",
                "Ana Pérez", "2221234567", "ENTREGA_LOCAL", direccion,
                List.of(new CrearOrdenCheckoutRequest.ProductoOrdenRequest(12L, BigDecimal.ONE)), List.of());

        OrdenCheckoutResponse respuesta = servicio.crear("nail-art", "entrega-local-1", request);

        assertThat(respuesta.costoEnvio()).isEqualByComparingTo("49.00");
        assertThat(respuesta.total()).isEqualByComparingTo("209.00");
        verify(ordenes).save(argThat(orden -> "ENTREGA_LOCAL".equals(orden.getModalidadEntrega())
                && "Reforma".equals(orden.getCalle()) && "72000".equals(orden.getCodigoPostal())
                && orden.getCostoEnvio().compareTo(new BigDecimal("49.00")) == 0));
    }

    @Test
    void noPermiteUsarUnaCitaDeOtroTenant() {
        UsuarioEntidad cliente = new UsuarioEntidad(); cliente.setId(70L); cliente.setEmpresaId(25L); cliente.setCorreo("cliente@test.mx");
        when(usuarios.findByEmpresaIdAndCorreo(25L, "cliente@test.mx")).thenReturn(Optional.of(cliente));
        CitaEntidad citaAjena = new CitaEntidad(); citaAjena.setId(91L); citaAjena.setEmpresaId(99L); citaAjena.setClienteId(70L);
        when(citas.findAllById(List.of(91L))).thenReturn(List.of(citaAjena));
        when(cotizador.cotizar(25L, 3L, null, java.util.Map.of()))
                .thenReturn(new ServicioCotizacionProductos.ResultadoCotizacion(null, List.of()));
        CrearOrdenCheckoutRequest request = new CrearOrdenCheckoutRequest(3L, null, "cliente@test.mx",
                "Ana Pérez", "2221234567", "RECOGE_EN_SUCURSAL", null, List.of(), List.of(91L));

        assertThatThrownBy(() -> servicio.crear("nail-art", "cita-ajena", request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no pertenecen al cliente o al negocio");
        verify(ordenes, never()).save(any());
    }

    @Test
    void reutilizaLaOrdenCuandoSeRepiteLaIdempotencia() {
        CheckoutOrdenEntidad existente = new CheckoutOrdenEntidad();
        existente.setId(44L); existente.setPublicId("orden-existente"); existente.setEmpresaId(25L);
        existente.setSucursalId(3L); existente.setEstado("PENDIENTE_PAGO"); existente.setMoneda("MXN");
        existente.setSubtotal(new BigDecimal("80.00")); existente.setDescuentoTotal(new BigDecimal("0.00"));
        existente.setImpuestosTotal(new BigDecimal("0.00")); existente.setTotal(new BigDecimal("80.00"));
        existente.setCostoEnvio(BigDecimal.ZERO); existente.setModalidadEntrega("RECOGE_EN_SUCURSAL");
        existente.setExpiraEn(java.time.LocalDateTime.now().plusMinutes(10));
        when(ordenes.findByEmpresaIdAndIdempotencia(25L, "misma-clave")).thenReturn(Optional.of(existente));

        OrdenCheckoutResponse respuesta = servicio.crear("nail-art", "misma-clave",
                new CrearOrdenCheckoutRequest(3L, null, "cliente@test.mx", "Ana Pérez", "2221234567",
                        "RECOGE_EN_SUCURSAL", null, List.of(), List.of()));

        assertThat(respuesta.id()).isEqualTo("orden-existente");
        verifyNoInteractions(cotizador);
    }

    private ConfiguracionCheckoutPublicaResponse configHabilitada() {
        return new ConfiguracionCheckoutPublicaResponse(true, "MERCADO_PAGO", "PRUEBA", "public-key",
                true, false, false, true, true, true, false, BigDecimal.ZERO, false, BigDecimal.ZERO,
                "PAGO_COMPLETO", null, null, "MXN");
    }
}
