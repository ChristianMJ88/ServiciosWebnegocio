package com.techprotech.agenda.modulos.caja.aplicacion;

import com.techprotech.agenda.modulos.caja.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.productos.aplicacion.*;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

class ServicioVentasCajaListadoTest {
    final VentaRepositorio ventas=mock(VentaRepositorio.class);
    final VentaDetalleRepositorio detalles=mock(VentaDetalleRepositorio.class);
    final PagoVentaRepositorio pagos=mock(PagoVentaRepositorio.class);
    final ClienteRepositorio clientes=mock(ClienteRepositorio.class);
    final SucursalRepositorio sucursales=mock(SucursalRepositorio.class);
    final ServicioVentasCaja servicio=new ServicioVentasCaja(ventas,detalles,pagos,mock(CajaSesionRepositorio.class),
            mock(MovimientoCajaRepositorio.class),mock(PagoCitaRepositorio.class),mock(CitaRepositorio.class),clientes,
            mock(ServicioRepositorio.class),mock(ProductoRepositorio.class),mock(ProductoExistenciaRepositorio.class),
            sucursales,mock(ServicioInventarioProductos.class),mock(ReglaDescuentoProductoRepositorio.class),
            mock(ZonaPrecioProductoRepositorio.class),mock(PrecioZonaProductoRepositorio.class),mock(ServicioComisionesDevengadas.class));
    void sucursal() {
        var sucursal=new SucursalEntidad(); sucursal.setId(3L);
        when(sucursales.findByIdAndEmpresaIdAndActivaTrue(3L,1L)).thenReturn(Optional.of(sucursal));
    }
    @Test void muestraPagoPosteriorAunqueElBatchNoEsteOrdenado() {
        sucursal(); var venta=new VentaEntidad(); venta.setId(10L);
        when(ventas.findTop50ByEmpresaIdAndSucursalIdOrderByCreadaEnDesc(1L,3L)).thenReturn(List.of(venta));
        var anterior=new PagoVentaEntidad(); anterior.setId(1L); anterior.setVentaId(10L);
        anterior.setRegistradoEn(LocalDateTime.of(2026,10,12,9,0)); anterior.setMetodoPago("EFECTIVO");
        var posterior=new PagoVentaEntidad(); posterior.setId(2L); posterior.setVentaId(10L);
        posterior.setRegistradoEn(anterior.getRegistradoEn().plusHours(1)); posterior.setMetodoPago("TARJETA");
        when(pagos.findByEmpresaIdAndVentaIdIn(eq(1L),any())).thenReturn(List.of(anterior,posterior));
        assertThat(servicio.listar(1L,List.of(3L),3L).getFirst().metodoPago()).isEqualTo("TARJETA");
    }
    @Test void paginaVaciaNoConsultaDetallesNiClientes() {
        sucursal();
        when(ventas.findByEmpresaIdAndSucursalIdAndEstadoAndCanalOrderByCreadaEnAsc(eq(1L),eq(3L),eq("POR_COBRAR"),eq("STAFF"),any()))
                .thenReturn(new PageImpl<>(List.of()));
        assertThat(servicio.pendientesStaff(1L,List.of(3L),3L,0)).isEmpty();
        verifyNoInteractions(detalles,clientes);
    }
    @Test void paginaConVentasSinClienteNoConsultaClientes() {
        sucursal();
        var venta = new VentaEntidad(); venta.setId(10L);
        when(ventas.findByEmpresaIdAndSucursalIdAndEstadoAndCanalOrderByCreadaEnAsc(eq(1L),eq(3L),eq("POR_COBRAR"),eq("STAFF"),any()))
                .thenReturn(new PageImpl<>(List.of(venta)));
        var resultado = servicio.pendientesStaff(1L,List.of(3L),3L,0);
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().getFirst().clienteNombre()).isEmpty();
        assertThat(resultado.getContent().getFirst().clienteTelefono()).isEmpty();
        verifyNoInteractions(clientes);
        verify(detalles).findByEmpresaIdAndVentaIdIn(1L,Set.of(10L));
    }
}
