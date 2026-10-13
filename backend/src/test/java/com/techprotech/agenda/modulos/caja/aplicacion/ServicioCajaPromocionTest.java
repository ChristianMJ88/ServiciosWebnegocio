package com.techprotech.agenda.modulos.caja.aplicacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

class ServicioCajaPromocionTest {
    @ParameterizedTest @ValueSource(strings={"promocion","normal","snapshot-invalido"})
    void exponePrecioPactadoSinRecalcularPromocion(String caso) {
        boolean promocion = !"normal".equals(caso);
        var citas=mock(CitaRepositorio.class);
        var sucursales=mock(SucursalRepositorio.class);
        var servicio=new ServicioCaja(mock(CajaSesionRepositorio.class),mock(PagoCitaRepositorio.class),
                mock(PagoVentaRepositorio.class),mock(VentaDetalleRepositorio.class),mock(MovimientoCajaRepositorio.class),
                citas,mock(ClienteRepositorio.class),mock(ServicioRepositorio.class),sucursales,new ObjectMapper());
        var sucursal=new SucursalEntidad(); sucursal.setId(3L); sucursal.setNombre("Sucursal de prueba"); sucursal.setZonaHoraria("America/Mexico_City");
        when(sucursales.findAllById(any())).thenReturn(List.of(sucursal));
        var cita=new CitaEntidad(); cita.setId(10L); cita.setClienteId(20L); cita.setServicioId(30L);
        cita.setSucursalId(3L); cita.setEstado("FINALIZADA"); cita.setMoneda("MXN");
        cita.setInicio(LocalDateTime.now(ZoneId.of(sucursal.getZonaHoraria())));
        cita.setPrecio(new BigDecimal("200")); cita.setPrecioLista(new BigDecimal(promocion?"250":"200"));
        cita.setDescuento(new BigDecimal(promocion?"50":"0"));
        if(promocion) cita.setPromocionSnapshotJson("{\"id\":8,\"titulo\":\"Oferta pactada\"}");
        if("snapshot-invalido".equals(caso)) cita.setPromocionSnapshotJson("{");
        when(citas.findByEmpresaIdAndEstadoInOrderByInicioDesc(eq(1L),any())).thenReturn(List.of(cita));
        var respuesta=servicio.listarCitasPorCobrar(1L,List.of(3L),3L).getFirst();
        assertThat(respuesta.precioLista()).isEqualByComparingTo(promocion?"250":"200");
        assertThat(respuesta.descuento()).isEqualByComparingTo(promocion?"50":"0");
        assertThat(respuesta.pendiente()).isEqualByComparingTo("200");
        assertThat(respuesta.promocionTitulo()).isEqualTo("promocion".equals(caso)?"Oferta pactada":null);
    }
}
