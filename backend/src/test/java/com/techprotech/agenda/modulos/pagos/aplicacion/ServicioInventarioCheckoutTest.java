package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenDetalleEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenDetalleRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenRepositorio;
import com.techprotech.agenda.modulos.productos.infraestructura.entidad.ProductoEntidad;
import com.techprotech.agenda.modulos.productos.infraestructura.entidad.ProductoExistenciaEntidad;
import com.techprotech.agenda.modulos.productos.infraestructura.entidad.ProductoExistenciaId;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.MovimientoInventarioRepositorio;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.ProductoExistenciaRepositorio;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.ProductoRepositorio;
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

class ServicioInventarioCheckoutTest {
    private CheckoutOrdenRepositorio ordenes;
    private CheckoutOrdenDetalleRepositorio detalles;
    private ProductoExistenciaRepositorio existencias;
    private MovimientoInventarioRepositorio movimientos;
    private CheckoutOrdenEntidad orden;
    private ProductoExistenciaEntidad existencia;
    private ServicioInventarioCheckout servicio;

    @BeforeEach
    void preparar() {
        ordenes = mock(CheckoutOrdenRepositorio.class);
        detalles = mock(CheckoutOrdenDetalleRepositorio.class);
        ProductoRepositorio productos = mock(ProductoRepositorio.class);
        existencias = mock(ProductoExistenciaRepositorio.class);
        movimientos = mock(MovimientoInventarioRepositorio.class);
        servicio = new ServicioInventarioCheckout(ordenes, detalles, productos, existencias, movimientos);

        orden = new CheckoutOrdenEntidad();
        orden.setId(40L); orden.setPublicId("orden-publica"); orden.setEmpresaId(25L); orden.setSucursalId(3L);
        orden.setInventarioEstado("PENDIENTE");
        when(ordenes.bloquearPorEmpresaEId(25L, 40L)).thenReturn(Optional.of(orden));

        CheckoutOrdenDetalleEntidad linea = new CheckoutOrdenDetalleEntidad();
        linea.setProductoId(12L); linea.setTipoConcepto("PRODUCTO"); linea.setCantidad(new BigDecimal("2"));
        when(detalles.findByEmpresaIdAndCheckoutOrdenIdOrderByIdAsc(25L, 40L)).thenReturn(List.of(linea));

        ProductoEntidad producto = new ProductoEntidad();
        producto.setId(12L); producto.setEmpresaId(25L); producto.setNombre("Aceite");
        producto.setActivo(true); producto.setControlaInventario(true);
        when(productos.findByEmpresaIdAndIdIn(eq(25L), any())).thenReturn(List.of(producto));

        existencia = new ProductoExistenciaEntidad();
        existencia.setId(new ProductoExistenciaId(25L, 12L, 3L));
        existencia.setExistencia(new BigDecimal("5")); existencia.setReservado(BigDecimal.ZERO);
        when(existencias.bloquear(25L, 12L, 3L)).thenReturn(Optional.of(existencia));
    }

    @Test
    void reservaYConfirmaSinVenderDosVecesLaMismaExistencia() {
        servicio.reservar(25L, 40L);

        assertThat(existencia.getReservado()).isEqualByComparingTo("2");
        assertThat(orden.getInventarioEstado()).isEqualTo("RESERVADO");

        servicio.confirmar(25L, 40L);

        assertThat(existencia.getExistencia()).isEqualByComparingTo("3");
        assertThat(existencia.getReservado()).isEqualByComparingTo("0");
        assertThat(orden.getInventarioEstado()).isEqualTo("CONFIRMADO");
        verify(movimientos).save(argThat(movimiento -> movimiento.getTipo().equals("VENTA_ONLINE")
                && movimiento.getCreadoPorUsuarioId() == null
                && movimiento.getReferencia().equals("orden-publica")));
    }

    @Test
    void rechazaLaReservaCuandoOtraCompraYaTomoElStock() {
        existencia.setReservado(new BigDecimal("4"));

        assertThatThrownBy(() -> servicio.reservar(25L, 40L))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Aceite");
        assertThat(orden.getInventarioEstado()).isEqualTo("PENDIENTE");
        verify(movimientos, never()).save(any());
    }

    @Test void pagoTardioRecuperaStockLiberadoSinConsumirReservasAjenas() {
        orden.setInventarioEstado("LIBERADO");existencia.setReservado(new BigDecimal("2"));
        assertThat(servicio.confirmarSiDisponible(25L,40L)).isTrue();
        assertThat(existencia.getExistencia()).isEqualByComparingTo("3");
        assertThat(existencia.getReservado()).isEqualByComparingTo("2");
        assertThat(orden.getInventarioEstado()).isEqualTo("CONFIRMADO");
    }

    @Test void faltanteTardioDevuelveIncidenciaSinMutarExistencias() {
        orden.setInventarioEstado("LIBERADO");existencia.setReservado(new BigDecimal("4"));
        assertThat(servicio.confirmarSiDisponible(25L,40L)).isFalse();
        assertThat(existencia.getExistencia()).isEqualByComparingTo("5");
        verify(existencias,never()).save(any());verifyNoInteractions(movimientos);
    }
}
