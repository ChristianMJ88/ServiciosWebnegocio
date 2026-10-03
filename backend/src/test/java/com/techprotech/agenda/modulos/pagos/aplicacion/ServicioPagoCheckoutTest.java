package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.modulos.pagos.api.dto.ConfiguracionCheckoutPublicaResponse;
import com.techprotech.agenda.modulos.pagos.api.dto.CrearPagoCheckoutRequest;
import com.techprotech.agenda.modulos.pagos.api.dto.CrearPagoAlternativoCheckoutRequest;
import com.techprotech.agenda.modulos.pagos.dominio.*;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioConfigEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ServicioPagoCheckoutTest {
    private ServicioConfiguracionCheckout configuracion;
    private CheckoutOrdenRepositorio ordenes;
    private CheckoutOrdenDetalleRepositorio detalles;
    private IntentoPagoCheckoutRepositorio intentos;
    private ProveedorPagoRepositorio proveedores;
    private ProveedorPagosPort mercadoPago;
    private ServicioInventarioCheckout inventario;
    private ServicioEstadoPagoCheckout estados;
    private ServicioMetodosPagoCheckout metodosPago;
    private ServicioPagoCheckout servicio;

    @BeforeEach
    void preparar() {
        configuracion = mock(ServicioConfiguracionCheckout.class);
        EmpresaSitioConfigRepositorio sitios = mock(EmpresaSitioConfigRepositorio.class);
        EmpresaCheckoutConfigRepositorio checkoutConfigs = mock(EmpresaCheckoutConfigRepositorio.class);
        proveedores = mock(ProveedorPagoRepositorio.class);
        ordenes = mock(CheckoutOrdenRepositorio.class);
        detalles = mock(CheckoutOrdenDetalleRepositorio.class);
        intentos = mock(IntentoPagoCheckoutRepositorio.class);
        mercadoPago = mock(ProveedorPagosPort.class);
        inventario = mock(ServicioInventarioCheckout.class);
        estados = mock(ServicioEstadoPagoCheckout.class);
        metodosPago = mock(ServicioMetodosPagoCheckout.class);

        EmpresaSitioConfigEntidad sitio = new EmpresaSitioConfigEntidad();
        sitio.setEmpresaId(25L); sitio.setSlug("nail-art"); sitio.setPublicado(true);
        when(sitios.findBySlug("nail-art")).thenReturn(Optional.of(sitio));
        when(configuracion.obtenerPublica("nail-art")).thenReturn(configHabilitada());

        EmpresaCheckoutConfigEntidad checkout = new EmpresaCheckoutConfigEntidad();
        checkout.setEmpresaId(25L); checkout.setProveedorPredeterminadoId(7L); checkout.setHabilitado(true);
        when(checkoutConfigs.findById(25L)).thenReturn(Optional.of(checkout));
        ProveedorPagoEntidad proveedor = new ProveedorPagoEntidad();
        proveedor.setId(7L); proveedor.setCodigo("MERCADO_PAGO"); proveedor.setActivo(true);
        when(proveedores.findById(7L)).thenReturn(Optional.of(proveedor));
        when(mercadoPago.proveedor()).thenReturn(ProveedorPago.MERCADO_PAGO);
        when(intentos.save(any())).thenAnswer(inv -> { IntentoPagoCheckoutEntidad item = inv.getArgument(0); item.setId(55L); return item; });

        servicio = new ServicioPagoCheckout(configuracion, sitios, checkoutConfigs, proveedores,
                ordenes, detalles, intentos, List.of(mercadoPago), inventario, estados, metodosPago,
                comisionesSinCargo());
    }

    private ServicioComisionPlataforma comisionesSinCargo() {
        ServicioComisionPlataforma servicio = mock(ServicioComisionPlataforma.class);
        when(servicio.calcular(anyLong(), anyLong(), any(), anyString())).thenReturn(BigDecimal.ZERO);
        return servicio;
    }

    @Test
    void cobraElImportePersistidoSinAceptarImportesDelNavegador() {
        CheckoutOrdenEntidad orden = ordenPendiente();
        when(ordenes.findByEmpresaIdAndPublicId(25L, "orden-123")).thenReturn(Optional.of(orden));
        when(mercadoPago.crearCobro(any())).thenReturn(new ResultadoCobro(ProveedorPago.MERCADO_PAGO,
                "mp-order-9", "mp-payment-8", EstadoPago.APROBADO, null, "approved", "Aprobado", OffsetDateTime.now()));
        IntentoPagoCheckoutEntidad aplicado = new IntentoPagoCheckoutEntidad();
        aplicado.setId(55L); aplicado.setEstado("APROBADO"); aplicado.setImporte(new BigDecimal("480.00"));
        aplicado.setMoneda("MXN"); aplicado.setProveedorPagoId(7L); aplicado.setReferenciaExterna("mp-order-9");
        aplicado.setReferenciaPagoExterna("mp-payment-8");
        when(estados.aplicar(eq(55L), any())).thenReturn(
                new ServicioEstadoPagoCheckout.EstadoAplicado("orden-123", "PAGADA", aplicado));

        var respuesta = servicio.pagarConTarjeta("nail-art", "orden-123", "pago-abc", request());

        assertThat(respuesta.estadoOrden()).isEqualTo("PAGADA");
        assertThat(respuesta.importe()).isEqualByComparingTo("480.00");
        assertThat(respuesta.referenciaPagoExterna()).isEqualTo("mp-payment-8");
        verify(mercadoPago).crearCobro(argThat(cobro -> cobro.importe().compareTo(new BigDecimal("480.00")) == 0
                && cobro.empresaId().equals(25L) && cobro.referenciaOrden().equals("orden-123")
                && cobro.correoPagador().equals("cliente@test.mx")));
        verify(intentos).save(argThat(intento -> intento.getEstado().equals("CREADO")
                && intento.getProveedorPagoId().equals(7L)));
        verify(inventario).reservar(25L, 41L);
        verify(estados).aplicar(eq(55L), any());
    }

    @Test
    void noLlamaAlProveedorCuandoLaOrdenExpiro() {
        CheckoutOrdenEntidad orden = ordenPendiente();
        orden.setExpiraEn(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1));
        when(ordenes.findByEmpresaIdAndPublicId(25L, "orden-123")).thenReturn(Optional.of(orden));

        assertThatThrownBy(() -> servicio.pagarConTarjeta("nail-art", "orden-123", "pago-expirado", request()))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("expiró");
        verifyNoInteractions(mercadoPago);
    }

    @Test
    void rechazaUnaClaveIdempotenteQuePerteneceAOtraOrden() {
        IntentoPagoCheckoutEntidad intento = new IntentoPagoCheckoutEntidad();
        intento.setCheckoutOrdenId(99L); intento.setEmpresaId(25L); intento.setProveedorPagoId(7L);
        intento.setIdempotencia("repetida"); intento.setEstado("APROBADO"); intento.setImporte(new BigDecimal("10.00"));
        intento.setMoneda("MXN");
        when(intentos.findByEmpresaIdAndIdempotencia(25L, "repetida")).thenReturn(Optional.of(intento));
        CheckoutOrdenEntidad otra = ordenPendiente(); otra.setId(99L); otra.setPublicId("otra-orden");
        when(ordenes.findById(99L)).thenReturn(Optional.of(otra));

        assertThatThrownBy(() -> servicio.pagarConTarjeta("nail-art", "orden-123", "repetida", request()))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("otra orden");
        verifyNoInteractions(mercadoPago);
    }

    @Test
    void rechazaPagoAlternativoCuandoLaOrdenIncluyeServicios() {
        CheckoutOrdenEntidad orden = ordenPendiente();
        when(ordenes.findByEmpresaIdAndPublicId(25L, "orden-123")).thenReturn(Optional.of(orden));
        CheckoutOrdenDetalleEntidad detalleServicio = new CheckoutOrdenDetalleEntidad();
        detalleServicio.setTipoConcepto("SERVICIO");
        when(detalles.findByEmpresaIdAndCheckoutOrdenIdOrderByIdAsc(25L, 41L)).thenReturn(List.of(detalleServicio));

        assertThatThrownBy(() -> servicio.pagarConMetodoAlternativo("nail-art", "orden-123", "pago-oxxo",
                new CrearPagoAlternativoCheckoutRequest("metodo-proveedor", "cliente@test.mx")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("solo para productos");
        verifyNoInteractions(metodosPago);
        verify(mercadoPago, never()).crearCobro(any());
    }

    @Test
    void consultaElProveedorYActualizaUnPagoPendiente() {
        CheckoutOrdenEntidad orden = ordenPendiente();
        orden.setEstado("PAGO_EN_PROCESO");
        when(ordenes.findByEmpresaIdAndPublicId(25L, "orden-123")).thenReturn(Optional.of(orden));
        IntentoPagoCheckoutEntidad intento = new IntentoPagoCheckoutEntidad();
        intento.setId(55L); intento.setCheckoutOrdenId(41L); intento.setEmpresaId(25L);
        intento.setProveedorPagoId(7L); intento.setEstado("REQUIERE_ACCION");
        intento.setReferenciaExterna("mp-order-9"); intento.setImporte(new BigDecimal("480.00")); intento.setMoneda("MXN");
        when(intentos.findFirstByEmpresaIdAndCheckoutOrdenIdOrderByIdDesc(25L, 41L)).thenReturn(Optional.of(intento));
        ResultadoCobro aprobado = new ResultadoCobro(ProveedorPago.MERCADO_PAGO, "mp-order-9", "mp-payment-8",
                EstadoPago.APROBADO, null, "approved", "Aprobado", OffsetDateTime.now());
        when(mercadoPago.consultarCobro(25L, "mp-order-9")).thenReturn(aprobado);
        IntentoPagoCheckoutEntidad intentoAplicado = new IntentoPagoCheckoutEntidad();
        intentoAplicado.setId(55L); intentoAplicado.setCheckoutOrdenId(41L); intentoAplicado.setEmpresaId(25L);
        intentoAplicado.setProveedorPagoId(7L); intentoAplicado.setEstado("APROBADO");
        intentoAplicado.setReferenciaExterna("mp-order-9"); intentoAplicado.setReferenciaPagoExterna("mp-payment-8");
        intentoAplicado.setImporte(new BigDecimal("480.00")); intentoAplicado.setMoneda("MXN");
        when(estados.aplicar(55L, aprobado)).thenReturn(
                new ServicioEstadoPagoCheckout.EstadoAplicado("orden-123", "PAGADA", intentoAplicado));

        var respuesta = servicio.consultar("nail-art", "orden-123");

        assertThat(respuesta.estadoOrden()).isEqualTo("PAGADA");
        assertThat(respuesta.estadoPago()).isEqualTo("APROBADO");
        verify(mercadoPago).consultarCobro(25L, "mp-order-9");
        verify(estados).aplicar(55L, aprobado);
    }

    private CheckoutOrdenEntidad ordenPendiente() {
        CheckoutOrdenEntidad orden = new CheckoutOrdenEntidad();
        orden.setId(41L); orden.setPublicId("orden-123"); orden.setEmpresaId(25L); orden.setSucursalId(3L);
        orden.setEstado("PENDIENTE_PAGO"); orden.setCorreoCliente("cliente@test.mx"); orden.setMoneda("MXN");
        orden.setNombreDestinatario("Cliente Prueba");
        orden.setInventarioEstado("PENDIENTE");
        orden.setSubtotal(new BigDecimal("480.00")); orden.setDescuentoTotal(BigDecimal.ZERO);
        orden.setImpuestosTotal(BigDecimal.ZERO); orden.setTotal(new BigDecimal("480.00"));
        orden.setExpiraEn(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));
        return orden;
    }

    private CrearPagoCheckoutRequest request() {
        return new CrearPagoCheckoutRequest("token-seguro", "visa", "credit_card", 1, "cliente@test.mx");
    }

    private ConfiguracionCheckoutPublicaResponse configHabilitada() {
        return new ConfiguracionCheckoutPublicaResponse(true, "MERCADO_PAGO", "PRUEBA", "public-key",
                true, false, false, true, true, true, true, false, BigDecimal.ZERO, false, BigDecimal.ZERO,
                false, "PAGO_COMPLETO", null, null, "MXN");
    }
}
