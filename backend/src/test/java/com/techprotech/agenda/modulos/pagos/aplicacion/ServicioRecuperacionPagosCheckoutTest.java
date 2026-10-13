package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.modulos.disponibilidad.aplicacion.ServicioConfiguracionReservas;
import com.techprotech.agenda.modulos.disponibilidad.api.dto.ConfiguracionReservasResponse;
import com.techprotech.agenda.modulos.pagos.dominio.*;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class ServicioRecuperacionPagosCheckoutTest {
    @Test void intentoViejoSinFechaConsultaAlProveedorAntesDeLiberarConTopeDelTenant() {
        var intentos=mock(IntentoPagoCheckoutRepositorio.class);
        var proveedores=mock(ProveedorPagoRepositorio.class);
        var adaptador=mock(ProveedorPagosPort.class);
        var estados=mock(ServicioEstadoPagoCheckout.class);
        var expiracion=mock(ServicioExpiracionCheckout.class);
        var reservas=mock(ServicioConfiguracionReservas.class);
        var reloj=Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"),ZoneOffset.UTC);
        var intento=new IntentoPagoCheckoutEntidad();intento.setId(8L);intento.setEmpresaId(7L);intento.setCheckoutOrdenId(9L);
        intento.setProveedorPagoId(2L);intento.setEstado("PROCESANDO");intento.setReferenciaExterna("orden-mp");
        intento.setCreadoEn(LocalDateTime.now(reloj).minusHours(3));
        when(intentos.buscarIdsParaRecuperacion(any(),any())).thenReturn(List.of(8L));
        when(intentos.findById(8L)).thenReturn(Optional.of(intento));
        when(reservas.obtener(7L)).thenReturn(new ConfiguracionReservasResponse(0,30,2,15,2));
        var proveedor=new ProveedorPagoEntidad();proveedor.setCodigo("MERCADO_PAGO");
        when(proveedores.findById(2L)).thenReturn(Optional.of(proveedor));
        when(adaptador.proveedor()).thenReturn(ProveedorPago.MERCADO_PAGO);
        when(adaptador.consultarCobro(7L,"orden-mp")).thenReturn(new ResultadoCobro(ProveedorPago.MERCADO_PAGO,"orden-mp","pago",EstadoPago.PROCESANDO,null,null,null,OffsetDateTime.now(reloj)));
        new ServicioRecuperacionPagosCheckout(intentos,proveedores,List.of(adaptador),estados,expiracion,reservas,reloj).recuperar();
        var orden=inOrder(adaptador,estados,expiracion);
        orden.verify(adaptador).consultarCobro(7L,"orden-mp");
        orden.verify(estados).aplicar(eq(8L),argThat(r -> r.expiraEn().toInstant().equals(reloj.instant().minus(Duration.ofHours(1))) && r.estado()==EstadoPago.PROCESANDO));
        orden.verify(expiracion).expirarEnProceso(7L,9L,8L,LocalDateTime.now(reloj).minusHours(1));
    }
}
