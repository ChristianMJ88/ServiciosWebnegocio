package com.techprotech.agenda.modulos.caja.aplicacion;

import com.techprotech.agenda.modulos.caja.infraestructura.entidad.PagoVentaEntidad;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.VentaDetalleEntidad;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.VentaEntidad;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.PagoVentaRepositorio;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.VentaDetalleRepositorio;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.VentaRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenDetalleEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.IntentoPagoCheckoutEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenDetalleRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.IntentoPagoCheckoutRepositorio;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class ServicioVentasCheckoutTest {
    @Test
    void proyectaPagoAprobadoComoVentaOnlineSinSesionNiUsuarioArtificial() {
        CheckoutOrdenRepositorio ordenes = mock(CheckoutOrdenRepositorio.class);
        CheckoutOrdenDetalleRepositorio detallesCheckout = mock(CheckoutOrdenDetalleRepositorio.class);
        IntentoPagoCheckoutRepositorio intentos = mock(IntentoPagoCheckoutRepositorio.class);
        VentaRepositorio ventas = mock(VentaRepositorio.class);
        VentaDetalleRepositorio detallesVenta = mock(VentaDetalleRepositorio.class);
        PagoVentaRepositorio pagos = mock(PagoVentaRepositorio.class);
        ServicioVentasCheckout servicio = new ServicioVentasCheckout(
                ordenes, detallesCheckout, intentos, ventas, detallesVenta, pagos);

        CheckoutOrdenEntidad orden = new CheckoutOrdenEntidad();
        orden.setId(3L); orden.setPublicId("orden-publica"); orden.setEmpresaId(25L); orden.setSucursalId(1L);
        orden.setEstado("PAGADA"); orden.setMoneda("MXN"); orden.setSubtotal(new BigDecimal("185.00"));
        orden.setDescuentoTotal(BigDecimal.ZERO); orden.setImpuestosTotal(BigDecimal.ZERO); orden.setTotal(new BigDecimal("185.00"));
        CheckoutOrdenDetalleEntidad linea = new CheckoutOrdenDetalleEntidad();
        linea.setTipoConcepto("PRODUCTO"); linea.setProductoId(8L); linea.setNombre("Producto");
        linea.setCantidad(BigDecimal.ONE); linea.setPrecioUnitario(new BigDecimal("185.00"));
        linea.setDescuento(BigDecimal.ZERO); linea.setImpuestos(BigDecimal.ZERO); linea.setTotal(new BigDecimal("185.00"));
        IntentoPagoCheckoutEntidad intento = new IntentoPagoCheckoutEntidad();
        intento.setId(9L); intento.setImporte(new BigDecimal("185.00")); intento.setReferenciaExterna("MP-ORDER-1");

        when(ordenes.bloquearPorEmpresaEId(25L, 3L)).thenReturn(Optional.of(orden));
        when(ventas.existsByEmpresaIdAndCheckoutOrdenId(25L, 3L)).thenReturn(false);
        when(intentos.findFirstByEmpresaIdAndCheckoutOrdenIdAndEstadoOrderByIdDesc(25L, 3L, "APROBADO"))
                .thenReturn(Optional.of(intento));
        when(ventas.save(any())).thenAnswer(inv -> { VentaEntidad venta = inv.getArgument(0); venta.setId(40L); return venta; });
        when(detallesCheckout.findByEmpresaIdAndCheckoutOrdenIdOrderByIdAsc(25L, 3L)).thenReturn(List.of(linea));

        servicio.proyectar(25L, 3L);

        verify(ventas).save(argThat(venta -> "ONLINE".equals(venta.getCanal())
                && venta.getCajaSesionId() == null && venta.getCreadaPorUsuarioId() == null
                && venta.getCheckoutOrdenId().equals(3L)));
        verify(detallesVenta).saveAll(argThat(items -> {
            VentaDetalleEntidad detalle = items.iterator().next();
            return detalle.getProductoId().equals(8L) && detalle.getVentaId().equals(40L);
        }));
        verify(pagos).save(argThat(pago -> "MERCADO_PAGO".equals(pago.getMetodoPago())
                && pago.getCajaSesionId() == null && pago.getRegistradoPorUsuarioId() == null
                && pago.getIntentoPagoCheckoutId().equals(9L)));
    }

    @Test
    void noDuplicaUnaVentaYaProyectada() {
        CheckoutOrdenRepositorio ordenes = mock(CheckoutOrdenRepositorio.class);
        CheckoutOrdenDetalleRepositorio detallesCheckout = mock(CheckoutOrdenDetalleRepositorio.class);
        IntentoPagoCheckoutRepositorio intentos = mock(IntentoPagoCheckoutRepositorio.class);
        VentaRepositorio ventas = mock(VentaRepositorio.class);
        VentaDetalleRepositorio detallesVenta = mock(VentaDetalleRepositorio.class);
        PagoVentaRepositorio pagos = mock(PagoVentaRepositorio.class);
        ServicioVentasCheckout servicio = new ServicioVentasCheckout(
                ordenes, detallesCheckout, intentos, ventas, detallesVenta, pagos);
        CheckoutOrdenEntidad orden = new CheckoutOrdenEntidad();
        orden.setId(3L); orden.setEmpresaId(25L); orden.setEstado("PAGADA");
        when(ordenes.bloquearPorEmpresaEId(25L, 3L)).thenReturn(Optional.of(orden));
        when(ventas.existsByEmpresaIdAndCheckoutOrdenId(25L, 3L)).thenReturn(true);

        servicio.proyectar(25L, 3L);

        verify(ventas, never()).save(any());
        verifyNoInteractions(detallesCheckout, intentos, detallesVenta, pagos);
    }
}
