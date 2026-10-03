package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.modulos.pagos.dominio.DesgloseLiquidacionPago;
import com.techprotech.agenda.modulos.pagos.dominio.EstadoPago;
import com.techprotech.agenda.modulos.pagos.dominio.ProveedorPago;
import com.techprotech.agenda.modulos.pagos.dominio.ResultadoCobro;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.ConciliacionPagoEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.IntentoPagoCheckoutEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.ConciliacionPagoRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.ProveedorPagoRepositorio;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ServicioConciliacionPagosTest {
    @Test
    void registraLaLiquidacionEnElTenantDelIntentoYUsaLosImportesDelProveedor() {
        ConciliacionPagoRepositorio repositorio = mock(ConciliacionPagoRepositorio.class);
        ServicioConciliacionPagos servicio = new ServicioConciliacionPagos(repositorio, mock(ProveedorPagoRepositorio.class));
        when(repositorio.findByProveedorPagoIdAndReferenciaOrdenExterna(2L, "ORD-1")).thenReturn(Optional.empty());
        when(repositorio.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        IntentoPagoCheckoutEntidad intento = new IntentoPagoCheckoutEntidad();
        intento.setId(11L); intento.setEmpresaId(7L); intento.setProveedorPagoId(2L); intento.setCheckoutOrdenId(21L);
        intento.setImporte(new BigDecimal("500.00")); intento.setMoneda("MXN");
        var desglose = new DesgloseLiquidacionPago("MXN", new BigDecimal("500.00"), new BigDecimal("18.56"),
                null, new BigDecimal("5.00"), null, null, BigDecimal.ZERO, new BigDecimal("476.44"), true,
                OffsetDateTime.parse("2026-10-03T12:30:00Z"));
        ResultadoCobro resultado = new ResultadoCobro(ProveedorPago.MERCADO_PAGO, "ORD-1", "PAY-1",
                EstadoPago.APROBADO, null, "approved", "accredited", OffsetDateTime.now(), desglose);

        servicio.registrarCheckout(intento, resultado);

        var captor = org.mockito.ArgumentCaptor.forClass(ConciliacionPagoEntidad.class);
        verify(repositorio).save(captor.capture());
        ConciliacionPagoEntidad guardada = captor.getValue();
        assertThat(guardada.getEmpresaId()).isEqualTo(7L);
        assertThat(guardada.getCanal()).isEqualTo("CHECKOUT");
        assertThat(guardada.getEstado()).isEqualTo("CONCILIADA");
        assertThat(guardada.getComisionProveedorTotal()).isEqualByComparingTo("18.56");
        assertThat(guardada.getComisionPlataforma()).isEqualByComparingTo("5.00");
        assertThat(guardada.getImporteNeto()).isEqualByComparingTo("476.44");
        assertThat(guardada.getImpuestoComisionProveedor()).isNull();
    }

    @Test
    void dejaPendienteCuandoMercadoPagoAunNoInformaElNeto() {
        ConciliacionPagoRepositorio repositorio = mock(ConciliacionPagoRepositorio.class);
        ServicioConciliacionPagos servicio = new ServicioConciliacionPagos(repositorio, mock(ProveedorPagoRepositorio.class));
        when(repositorio.findByProveedorPagoIdAndReferenciaOrdenExterna(2L, "ORD-2")).thenReturn(Optional.empty());
        IntentoPagoCheckoutEntidad intento = new IntentoPagoCheckoutEntidad();
        intento.setId(12L); intento.setEmpresaId(8L); intento.setProveedorPagoId(2L); intento.setCheckoutOrdenId(22L);
        intento.setImporte(new BigDecimal("200.00")); intento.setMoneda("MXN");

        servicio.registrarCheckout(intento, new ResultadoCobro(ProveedorPago.MERCADO_PAGO, "ORD-2", "PAY-2",
                EstadoPago.APROBADO, null, "approved", "accredited", OffsetDateTime.now()));

        var captor = org.mockito.ArgumentCaptor.forClass(ConciliacionPagoEntidad.class);
        verify(repositorio).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo("PENDIENTE_CONCILIACION");
        assertThat(captor.getValue().getImporteNeto()).isNull();
        assertThat(captor.getValue().isLiquidacionDefinitiva()).isFalse();
    }
}
