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
import com.techprotech.agenda.modulos.envios.aplicacion.ServicioCotizacionesEnvio;
import com.techprotech.agenda.modulos.pagos.api.dto.*;
import com.techprotech.agenda.modulos.pagos.infraestructura.checkout.PropiedadesCheckout;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenDetalleEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenDetalleRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenRepositorio;
import com.techprotech.agenda.modulos.productos.aplicacion.ServicioCotizacionProductos;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
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
    private ServicioRepositorio servicios;
    private UsuarioRepositorio usuarios;
    private CheckoutOrdenRepositorio ordenes;
    private CheckoutOrdenDetalleRepositorio detalles;
    private ServicioOrdenCheckout servicio;
    private ServicioCotizacionesEnvio cotizacionesEnvio;
    private PagoCitaRepositorio pagosCita;
    private VentaDetalleRepositorio detallesVenta;

    @BeforeEach
    void preparar() {
        configuracion = mock(ServicioConfiguracionCheckout.class);
        EmpresaSitioConfigRepositorio sitios = mock(EmpresaSitioConfigRepositorio.class);
        SucursalRepositorio sucursales = mock(SucursalRepositorio.class);
        cotizador = mock(ServicioCotizacionProductos.class);
        citas = mock(CitaRepositorio.class);
        servicios = mock(ServicioRepositorio.class);
        usuarios = mock(UsuarioRepositorio.class);
        pagosCita = mock(PagoCitaRepositorio.class);
        detallesVenta = mock(VentaDetalleRepositorio.class);
        ordenes = mock(CheckoutOrdenRepositorio.class);
        detalles = mock(CheckoutOrdenDetalleRepositorio.class);
        cotizacionesEnvio = mock(ServicioCotizacionesEnvio.class);
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
                usuarios, pagosCita, detallesVenta, ordenes, detalles, new PropiedadesCheckout(20, 3), direcciones,
                cotizacionesEnvio,mock(com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioCapacidadesEmpresa.class), productosCostos());
    }

    private com.techprotech.agenda.modulos.productos.infraestructura.repositorio.ProductoRepositorio productosCostos() {
        var repo = mock(com.techprotech.agenda.modulos.productos.infraestructura.repositorio.ProductoRepositorio.class);
        lenient().when(repo.findByIdAndEmpresaId(any(), any())).thenAnswer(i -> {
            var producto = new com.techprotech.agenda.modulos.productos.infraestructura.entidad.ProductoEntidad();
            producto.setId(i.getArgument(0)); producto.setEmpresaId(i.getArgument(1));
            producto.setCosto(new BigDecimal("35.00"));
            return java.util.Optional.of(producto);
        });
        return repo;
    }

    @Test
    void creaOrdenConPrecioCalculadoPorElBackend() {
        when(cotizador.cotizar(eq(25L), eq(3L), eq("CENTRO"), any())).thenReturn(
                new ServicioCotizacionProductos.ResultadoCotizacion(8L, List.of(
                        new ServicioCotizacionProductos.CotizacionProducto(12L, "Aceite", new BigDecimal("2"),
                                new BigDecimal("100.00"), new BigDecimal("80.00"), new BigDecimal("40.00"),
                                new BigDecimal("160.00"), "MXN"))));
        CrearOrdenCheckoutRequest request = new CrearOrdenCheckoutRequest(3L, "CENTRO", "cliente@test.mx",
                "Ana Pérez", "2221234567", "RECOGE_EN_SUCURSAL", null, null,
                List.of(new CrearOrdenCheckoutRequest.ProductoOrdenRequest(12L, new BigDecimal("2"))), List.of());

        OrdenCheckoutResponse respuesta = servicio.crear("nail-art", "carrito-123", request);

        verify(detalles).saveAll(argThat(items -> {
            var item = items.iterator().next();
            return item.getEmpresaId().equals(25L) && item.getProductoId().equals(12L)
                && new BigDecimal("35.00").equals(item.getCostoUnitarioHistorico());
        }));
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
                true, true, true, new BigDecimal("49.00"), true, new BigDecimal("129.00"),
                false, "PAGO_COMPLETO", null, null, "MXN"));
        when(cotizador.cotizar(eq(25L), eq(3L), isNull(), any())).thenReturn(
                new ServicioCotizacionProductos.ResultadoCotizacion(null, List.of(
                        new ServicioCotizacionProductos.CotizacionProducto(12L, "Aceite", BigDecimal.ONE,
                                new BigDecimal("160.00"), new BigDecimal("160.00"), BigDecimal.ZERO,
                                new BigDecimal("160.00"), "MXN"))));
        CrearOrdenCheckoutRequest.DireccionEntregaRequest direccion =
                new CrearOrdenCheckoutRequest.DireccionEntregaRequest("Reforma", "123", "4",
                        "Centro", "Puebla", "Puebla", "72000", "MX", "Portón blanco");
        CrearOrdenCheckoutRequest request = new CrearOrdenCheckoutRequest(3L, null, "cliente@test.mx",
                "Ana Pérez", "2221234567", "ENTREGA_LOCAL", null, direccion,
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
                "Ana Pérez", "2221234567", "RECOGE_EN_SUCURSAL", null, null, List.of(), List.of(91L));

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
                        "RECOGE_EN_SUCURSAL", null, null, List.of(), List.of()));

        assertThat(respuesta.id()).isEqualTo("orden-existente");
        verifyNoInteractions(cotizador);
    }

    @Test
    void respetaElAnticipoConfiguradoEnElServicioSobreLaModalidadGeneral() {
        UsuarioEntidad cliente = new UsuarioEntidad(); cliente.setId(70L); cliente.setEmpresaId(25L); cliente.setCorreo("cliente@test.mx");
        when(usuarios.findByEmpresaIdAndCorreo(25L, "cliente@test.mx")).thenReturn(Optional.of(cliente));
        CitaEntidad cita = new CitaEntidad();
        cita.setId(91L); cita.setEmpresaId(25L); cita.setClienteId(70L); cita.setSucursalId(3L);
        cita.setServicioId(12L); cita.setEstado("PENDIENTE"); cita.setPrecio(new BigDecimal("800.00")); cita.setMoneda("MXN");
        when(citas.findAllById(List.of(91L))).thenReturn(List.of(cita));
        ServicioEntidad servicioEntidad = new ServicioEntidad();
        servicioEntidad.setId(12L); servicioEntidad.setNombre("Coloración"); servicioEntidad.setRequiereAnticipo(true);
        servicioEntidad.setAnticipoTipo("PORCENTAJE"); servicioEntidad.setAnticipoValor(new BigDecimal("25.00"));
        when(servicios.findByEmpresaIdAndIdIn(eq(25L), any())).thenReturn(List.of(servicioEntidad));
        when(cotizador.cotizar(25L, 3L, null, java.util.Map.of()))
                .thenReturn(new ServicioCotizacionProductos.ResultadoCotizacion(null, List.of()));

        OrdenCheckoutResponse respuesta = servicio.crear("nail-art", "anticipo-servicio",
                new CrearOrdenCheckoutRequest(3L, null, "cliente@test.mx", "Ana Pérez", "2221234567",
                        "RECOGE_EN_SUCURSAL", null, null, List.of(), List.of(91L)));

        assertThat(respuesta.total()).isEqualByComparingTo("200.00");
        verify(detalles).saveAll(argThat(items -> {
            CheckoutOrdenDetalleEntidad detalle = ((List<CheckoutOrdenDetalleEntidad>) items).getFirst();
            return detalle.getCitaId().equals(91L) && detalle.getTotal().compareTo(new BigDecimal("200.00")) == 0;
        }));
    }

    private ConfiguracionCheckoutPublicaResponse configHabilitada() {
        return new ConfiguracionCheckoutPublicaResponse(true, "MERCADO_PAGO", "PRUEBA", "public-key",
                true, false, false, true, true, true, true, false, BigDecimal.ZERO, false, BigDecimal.ZERO,
                false, "PAGO_COMPLETO", null, null, "MXN");
    }

    @Test void anticipaCienSobreDoscientosNetosYNoDuplicaDescuentoEnSaldo() {
        UsuarioEntidad cliente = new UsuarioEntidad();cliente.setId(70L);
        when(usuarios.findByEmpresaIdAndCorreo(25L,"cliente@test.mx")).thenReturn(Optional.of(cliente));
        CitaEntidad cita=new CitaEntidad();cita.setId(91L);cita.setEmpresaId(25L);cita.setClienteId(70L);cita.setSucursalId(3L);cita.setServicioId(12L);cita.setEstado("PENDIENTE");cita.setMoneda("MXN");
        cita.setPrecio(new BigDecimal("200"));cita.setPrecioLista(new BigDecimal("250"));cita.setDescuento(new BigDecimal("50"));
        when(citas.findAllById(List.of(91L))).thenReturn(List.of(cita));
        ServicioEntidad concepto=new ServicioEntidad();concepto.setId(12L);concepto.setNombre("Servicio");concepto.setRequiereAnticipo(true);concepto.setAnticipoTipo("PORCENTAJE");concepto.setAnticipoValor(new BigDecimal("50"));
        when(servicios.findByEmpresaIdAndIdIn(eq(25L),any())).thenReturn(List.of(concepto));
        when(cotizador.cotizar(25L,3L,null,java.util.Map.of())).thenReturn(new ServicioCotizacionProductos.ResultadoCotizacion(null,List.of()));
        var request=new CrearOrdenCheckoutRequest(3L,null,"cliente@test.mx","Cliente prueba","2220000000","RECOGE_EN_SUCURSAL",null,null,List.of(),List.of(91L));
        var anticipo=servicio.crear("nail-art","anticipo-con-oferta",request);
        assertThat(anticipo.total()).isEqualByComparingTo("100");assertThat(anticipo.descuentoTotal()).isEqualByComparingTo("50");
        when(pagosCita.sumarPagadoPorCita(25L,91L)).thenReturn(new BigDecimal("100"));
        var saldo=servicio.crear("nail-art","saldo-con-oferta",request);
        assertThat(saldo.total()).isEqualByComparingTo("100");assertThat(saldo.descuentoTotal()).isZero();
        verify(detalles).saveAll(argThat(items -> ((List<CheckoutOrdenDetalleEntidad>) items).getFirst().getNombre().startsWith("Anticipo ·")));
        verify(detalles).saveAll(argThat(items -> ((List<CheckoutOrdenDetalleEntidad>) items).getFirst().getNombre().startsWith("Liquidar saldo ·")));
    }
}
