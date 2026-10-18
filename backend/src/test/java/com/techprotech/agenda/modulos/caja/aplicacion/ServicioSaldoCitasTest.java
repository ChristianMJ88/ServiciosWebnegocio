package com.techprotech.agenda.modulos.caja.aplicacion;

import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServicioSaldoCitasTest {
    final CitaRepositorio citas=mock(CitaRepositorio.class);
    final PagoCitaRepositorio pagos=mock(PagoCitaRepositorio.class);
    final VentaDetalleRepositorio ventas=mock(VentaDetalleRepositorio.class);
    final ServicioSaldoCitas servicio=new ServicioSaldoCitas(citas,pagos,ventas);

    @ParameterizedTest
    @CsvSource({"500,0,0,500,PENDIENTE", "500,0,500,0,PAGADA", "500,150,100,250,PARCIAL",
            "500,400,100,0,PAGADA", "0,0,0,0,SIN_COSTO", "500,450,100,0,PAGADA"})
    void saldoRespetaPrecioPactadoYAmbosOrigenesSinRedondearNiDuplicar(String precio,String caja,String venta,String pendiente,EstadoCobroCita estado) {
        var cita=cita(1L,7L,"FINALIZADA",precio);
        when(pagos.sumarPagadoPorCitas(7L,List.of(1L))).thenReturn(fila(1L,caja));
        when(ventas.sumarServiciosPagadosPorCitas(7L,List.of(1L))).thenReturn(fila(1L,venta));
        var saldo=servicio.de(cita);
        assertThat(saldo.precio()).isEqualByComparingTo(precio);
        assertThat(saldo.pagado()).isEqualByComparingTo(new BigDecimal(caja).add(new BigDecimal(venta)));
        assertThat(saldo.pagadoCaja()).isEqualByComparingTo(caja);
        assertThat(saldo.pagadoVenta()).isEqualByComparingTo(venta);
        assertThat(saldo.condonado()).isZero();
        assertThat(saldo.pendiente()).isEqualByComparingTo(pendiente);
        assertThat(saldo.estadoCobro()).isEqualTo(estado);
    }

    @ParameterizedTest @CsvSource({"NO_ASISTIO", "CANCELADA", "LIBERADA", "PENDIENTE_PAGO"})
    void noCobrableRetieneAnticipoPeroNoCreaDeuda(String estado) {
        when(ventas.sumarServiciosPagadosPorCitas(7L,List.of(1L))).thenReturn(fila(1L,"80.00"));
        var cita=cita(1L,7L,estado,"160.00");
        var saldo=servicio.de(cita);
        assertThat(saldo.pagado()).isEqualByComparingTo("80.00");
        assertThat(saldo.pendiente()).isZero();
        assertThat(saldo.estadoCobro()).isEqualTo(EstadoCobroCita.NO_COBRABLE);
        assertThat(ServicioSaldoCitas.esCobrable(cita)).isFalse();
    }

    @Test void loteValidaTenantDeduplicaYHaceSoloDosSumas() {
        var propias=java.util.stream.LongStream.rangeClosed(1,100).mapToObj(id -> cita(id,7L,"CONFIRMADA","80.55")).toList();
        var ids=propias.stream().map(CitaEntidad::getId).toList();
        var mezcladas=new ArrayList<>(propias);mezcladas.add(propias.getFirst());mezcladas.add(cita(900L,8L,"FINALIZADA","999"));
        var resultado=servicio.calcular(7L,mezcladas);
        assertThat(resultado).hasSize(100).doesNotContainKey(900L);
        assertThat(resultado.get(1L).pendiente()).isEqualByComparingTo("80.55");
        verify(pagos).sumarPagadoPorCitas(7L,ids);verifyNoMoreInteractions(pagos);
        verify(ventas).sumarServiciosPagadosPorCitas(7L,ids);verify(ventas).citasConPointEnCurso(7L,ids);verifyNoMoreInteractions(ventas);
        verifyNoInteractions(citas);
    }

    @Test void consultaIdsEstaAcotadaAlTenantYNoConsultaLotesVacios() {
        when(citas.findByEmpresaIdAndIdIn(7L,List.of(1L,2L))).thenReturn(List.of(cita(1L,7L,"FINALIZADA","80")));
        assertThat(servicio.saldos(7L,Arrays.asList(1L,2L,1L,null))).containsOnlyKeys(1L);
        verify(citas).findByEmpresaIdAndIdIn(7L,List.of(1L,2L));
        clearInvocations(citas,pagos,ventas);
        assertThat(servicio.saldos(7L,List.of())).isEmpty();
        assertThat(servicio.calcular(7L,List.of(cita(2L,8L,"FINALIZADA","999")))).isEmpty();
        verifyNoInteractions(citas,pagos,ventas);
    }

    @Test void pointApartaSoloEnEntradaDeCobroSinCambiarSaldoPresentado() {
        var cita=cita(1L,7L,"FINALIZADA","500");
        when(ventas.citasConPointEnCurso(7L,List.of(1L))).thenReturn(List.of(1L));
        var saldo=servicio.de(cita);
        assertThat(saldo.pendiente()).isEqualByComparingTo("500");
        assertThat(saldo.cobroPointEnCurso()).isTrue();
        assertThatThrownBy(() -> servicio.calcularBajoBloqueo(7L,List.of(cita)))
            .isInstanceOf(org.springframework.web.server.ResponseStatusException.class).hasMessageContaining("Point en curso");
        assertThat(servicio.calcularBajoBloqueo(7L,List.of(cita(2L,7L,"FINALIZADA","500")))).containsKey(2L);
    }

    static CitaEntidad cita(Long id,Long empresa,String estado,String precio) {
        var c=new CitaEntidad();c.setId(id);c.setEmpresaId(empresa);c.setEstado(estado);c.setPrecio(new BigDecimal(precio));c.setSucursalId(3L);return c;
    }
    static List<Object[]> fila(Long id,String valor) { return Collections.singletonList(new Object[]{id,new BigDecimal(valor)}); }
}
