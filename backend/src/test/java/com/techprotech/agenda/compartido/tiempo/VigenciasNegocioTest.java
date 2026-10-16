package com.techprotech.agenda.compartido.tiempo;

import com.techprotech.agenda.modulos.caja.aplicacion.ServicioVentasCaja;
import com.techprotech.agenda.modulos.caja.api.dto.*;
import com.techprotech.agenda.modulos.productos.aplicacion.*;
import com.techprotech.agenda.modulos.productos.api.dto.ConfiguracionTiendaResponse;
import com.techprotech.agenda.modulos.productos.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.sitio.aplicacion.ServicioPromocionesSitio;
import com.techprotech.agenda.modulos.sitio.dominio.TipoDescuentoPromocion;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Isolated("Compara vigencias con JVM UTC y México, restaurándola en finally")
@ExtendWith(MockitoExtension.class)
class VigenciasNegocioTest {
    @Mock Clock reloj;
    @Mock ZonaNegocio zonasNegocio;
    @Mock ProductoRepositorio productos;
    @Mock ReglaDescuentoProductoRepositorio descuentos;
    @Mock ProductoExistenciaRepositorio existencias;
    @Mock ZonaPrecioProductoRepositorio zonas;
    @Mock PrecioZonaProductoRepositorio precios;
    @Mock CategoriaProductoRepositorio categorias;
    @Mock ServicioRepositorio servicios;
    @Mock SucursalRepositorio sucursales;
    @Mock EmpresaSitioConfigRepositorio sitios;
    @Mock ServicioConfiguracionTienda tienda;
    @Mock EmpresaSitioPromocionRepositorio promociones;
    @Mock EmpresaSitioPromocionServicioRepositorio serviciosPromocion;
    @Mock EmpresaSitioPromocionSucursalRepositorio sucursalesPromocion;
    @InjectMocks ServicioCatalogoProductosPublico publico;
    @InjectMocks ServicioCotizacionProductos checkout;
    @InjectMocks ServicioVentasCaja caja;
    @InjectMocks ServicioPromocionesSitio sitio;

    @ParameterizedTest
    @CsvSource({"UTC,2026-10-14T01:00:00Z,true", "America/Mexico_City,2026-10-14T01:00:00Z,true",
            "UTC,2026-10-14T06:00:00Z,false", "America/Mexico_City,2026-10-14T06:00:00Z,false"})
    void descuentosPromocionesYNovedadesTerminanALaMedianocheDeLaSucursal(String jvm, String instante, boolean vigente) {
        var anterior = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(jvm));
            when(reloj.withZone(any())).thenAnswer(i -> Clock.fixed(Instant.parse(instante), i.getArgument(0)));
            when(zonasNegocio.de(7L,3L)).thenReturn(ZoneId.of("America/Mexico_City"));
            var sucursal = new SucursalEntidad(); sucursal.setId(3L); sucursal.setEmpresaId(7L);
            sucursal.setActiva(true); sucursal.setNombre("Sede");
            when(sucursales.findByEmpresaIdAndActivaTrue(7L)).thenReturn(List.of(sucursal));
            when(sucursales.findByEmpresaIdOrderByNombreAsc(7L)).thenReturn(List.of(sucursal));
            when(sucursales.findByIdAndEmpresaIdAndActivaTrue(3L,7L)).thenReturn(Optional.of(sucursal));
            var config = new EmpresaSitioConfigEntidad(); config.setEmpresaId(7L); config.setSlug("negocio"); config.setPublicado(true);
            when(sitios.findBySlug("negocio")).thenReturn(Optional.of(config));
            var opciones = new ConfiguracionTiendaResponse(true,true,null,null,null,null,null,true,false,false,0);
            when(tienda.obtener(7L)).thenReturn(opciones);
            var producto = new ProductoEntidad(); producto.setId(8L); producto.setEmpresaId(7L); producto.setNombre("Producto");
            producto.setSlug("producto"); producto.setPrecioBase(new BigDecimal("100")); producto.setMoneda("MXN");
            producto.setActivo(true); producto.setVisiblePublico(true);
            producto.setNuevoDesde(LocalDate.of(2026,10,13)); producto.setNuevoHasta(LocalDate.of(2026,10,13));
            when(productos.findByEmpresaIdAndActivoTrueAndVisiblePublicoTrueOrderByNombreAsc(7L)).thenReturn(List.of(producto));
            when(productos.findByEmpresaIdAndIdIn(7L,Set.of(8L))).thenReturn(List.of(producto));
            var regla = new ReglaDescuentoProductoEntidad(); regla.setEmpresaId(7L); regla.setNombre("Descuento");
            regla.setActiva(true); regla.setAlcance("GLOBAL"); regla.setSucursalId(3L);
            regla.setCantidadMinima(BigDecimal.ONE); regla.setTipoDescuento("PORCENTAJE"); regla.setValor(new BigDecimal("20"));
            regla.setFechaFin(LocalDate.of(2026,10,13));
            when(descuentos.findByEmpresaIdOrderByPrioridadDescNombreAsc(7L)).thenReturn(List.of(regla));
            var promo = new EmpresaSitioPromocionEntidad(); promo.setId(12L); promo.setEmpresaId(7L); promo.setActiva(true);
            promo.setTipoDescuento(TipoDescuentoPromocion.PORCENTAJE); promo.setValorDescuento(new BigDecimal("20"));
            promo.setFechaFin(LocalDate.of(2026,10,13));
            when(promociones.findByEmpresaIdOrderByOrdenAsc(7L)).thenReturn(List.of(promo));
            var precio = vigente ? "80.00" : "100.00";
            var publicado = publico.obtener("negocio",null,null,3L).productos().getFirst();
            assertThat(publicado.precio()).isEqualByComparingTo(precio);
            assertThat(publicado.nuevo()).isEqualTo(vigente);
            assertThat(checkout.cotizar(7L,3L,null,Map.of(8L,BigDecimal.ONE)).productos().getFirst().precioUnitario())
                    .isEqualByComparingTo(precio);
            var request = new CotizacionCajaRequest(3L,List.of(new RegistrarVentaRequest.ProductoVentaRequest(8L,BigDecimal.ONE)));
            assertThat(caja.cotizarProductos(7L,List.of(3L),request).total()).isEqualByComparingTo(precio);
            assertThat(sitio.obtenerParaSitioPublico(7L)).hasSize(vigente ? 1 : 0);
        } finally { TimeZone.setDefault(anterior); }
    }
}
