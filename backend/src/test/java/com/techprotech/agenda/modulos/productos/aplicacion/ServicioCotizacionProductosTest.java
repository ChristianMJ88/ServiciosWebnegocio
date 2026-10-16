package com.techprotech.agenda.modulos.productos.aplicacion;

import com.techprotech.agenda.modulos.productos.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ServicioCotizacionProductosTest {
    private ProductoRepositorio productos;
    private ZonaPrecioProductoRepositorio zonas;
    private PrecioZonaProductoRepositorio precios;
    private ReglaDescuentoProductoRepositorio descuentos;
    private ProductoExistenciaRepositorio existencias;
    private ServicioCotizacionProductos servicio;

    @BeforeEach
    void preparar() {
        productos = mock(ProductoRepositorio.class);
        zonas = mock(ZonaPrecioProductoRepositorio.class);
        precios = mock(PrecioZonaProductoRepositorio.class);
        descuentos = mock(ReglaDescuentoProductoRepositorio.class);
        existencias = mock(ProductoExistenciaRepositorio.class);
        servicio = new ServicioCotizacionProductos(productos, zonas, precios, descuentos, existencias, java.time.Clock.systemUTC(), com.techprotech.agenda.compartido.tiempo.TiempoPruebas.zonaUtc());
    }

    @Test
    void aplicaPrecioDeZonaYDescuentoPorCantidadDeLaSucursal() {
        ProductoEntidad producto = new ProductoEntidad();
        producto.setId(12L); producto.setEmpresaId(25L); producto.setNombre("Aceite"); producto.setCategoriaId(4L);
        producto.setPrecioBase(new BigDecimal("100.00")); producto.setMoneda("MXN"); producto.setActivo(true);
        producto.setVisiblePublico(true); producto.setControlaInventario(true);
        when(productos.findByEmpresaIdAndIdIn(25L, java.util.Set.of(12L))).thenReturn(List.of(producto));

        ZonaPrecioProductoEntidad zona = new ZonaPrecioProductoEntidad();
        zona.setId(8L); zona.setEmpresaId(25L); zona.setClave("NORTE"); zona.setActiva(true);
        when(zonas.findByEmpresaIdOrderByPrioridadAscNombreAsc(25L)).thenReturn(List.of(zona));
        PrecioZonaProductoEntidad precio = new PrecioZonaProductoEntidad();
        precio.setProductoId(12L); precio.setZonaPrecioId(8L); precio.setPrecio(new BigDecimal("90.00"));
        precio.setMoneda("MXN"); precio.setActivo(true);
        when(precios.findByEmpresaIdAndProductoIdIn(25L, java.util.Set.of(12L))).thenReturn(List.of(precio));

        ReglaDescuentoProductoEntidad regla = new ReglaDescuentoProductoEntidad();
        regla.setEmpresaId(25L); regla.setNombre("Mayoreo"); regla.setAlcance("PRODUCTO"); regla.setProductoId(12L);
        regla.setSucursalId(3L); regla.setTipoDescuento("PORCENTAJE"); regla.setValor(new BigDecimal("10"));
        regla.setCantidadMinima(new BigDecimal("2")); regla.setActiva(true); regla.setCombinable(false);
        regla.setFechaInicio(LocalDate.now().minusDays(1));
        when(descuentos.findByEmpresaIdOrderByPrioridadDescNombreAsc(25L)).thenReturn(List.of(regla));

        ProductoExistenciaEntidad existencia = new ProductoExistenciaEntidad();
        existencia.setId(new ProductoExistenciaId(25L, 12L, 3L)); existencia.setExistencia(new BigDecimal("10"));
        existencia.setReservado(new BigDecimal("1"));
        when(existencias.findByIdEmpresaIdAndIdSucursalIdIn(25L, List.of(3L))).thenReturn(List.of(existencia));

        ServicioCotizacionProductos.ResultadoCotizacion resultado = servicio.cotizar(
                25L, 3L, "NORTE", Map.of(12L, new BigDecimal("2")));

        assertThat(resultado.zonaPrecioId()).isEqualTo(8L);
        assertThat(resultado.productos()).singleElement().satisfies(linea -> {
            assertThat(linea.precioListaUnitario()).isEqualByComparingTo("90.00");
            assertThat(linea.precioUnitario()).isEqualByComparingTo("81.00");
            assertThat(linea.descuento()).isEqualByComparingTo("18.00");
            assertThat(linea.total()).isEqualByComparingTo("162.00");
        });
    }
}
