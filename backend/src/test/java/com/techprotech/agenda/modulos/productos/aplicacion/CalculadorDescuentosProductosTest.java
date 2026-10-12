package com.techprotech.agenda.modulos.productos.aplicacion;
import java.math.BigDecimal;import java.time.LocalDate;import java.util.List;
import org.junit.jupiter.api.Test;
import com.techprotech.agenda.modulos.productos.infraestructura.entidad.*;
import static org.junit.jupiter.api.Assertions.*;
class CalculadorDescuentosProductosTest {
 private ReglaDescuentoProductoEntidad regla(){var r=new ReglaDescuentoProductoEntidad();r.setActiva(true);r.setAlcance("PRODUCTO");r.setProductoId(5L);r.setSucursalId(3L);r.setTipoDescuento("PORCENTAJE");r.setValor(BigDecimal.TEN);r.setCantidadMinima(BigDecimal.ONE);r.setNombre("Temporada");return r;}
 @Test void descuentoSoloEnSucursalAplicable(){var p=new ProductoEntidad();p.setId(5L);var r=regla();assertEquals(new BigDecimal("166.50"),CalculadorDescuentosProductos.calcular(p,null,3L,BigDecimal.ONE,new BigDecimal("185"),"MXN",List.of(r)).precio());assertNull(CalculadorDescuentosProductos.calcular(p,null,4L,BigDecimal.ONE,new BigDecimal("185"),"MXN",List.of(r)).etiqueta());assertNull(CalculadorDescuentosProductos.calcular(p,null,null,BigDecimal.ONE,new BigDecimal("185"),"MXN",List.of(r)).etiqueta());}
 @Test void cantidadMinimaNoSePublicaComoDescuentoUnitario(){var p=new ProductoEntidad();p.setId(5L);var r=regla();r.setCantidadMinima(new BigDecimal("2"));assertNull(CalculadorDescuentosProductos.calcular(p,null,3L,BigDecimal.ONE,new BigDecimal("185"),"MXN",List.of(r)).etiqueta());assertEquals(new BigDecimal("166.50"),CalculadorDescuentosProductos.calcular(p,null,3L,new BigDecimal("2"),new BigDecimal("185"),"MXN",List.of(r)).precio());}
 @Test void vigenciaIncluyeFechaFinal(){var r=regla();r.setFechaInicio(LocalDate.of(2026,10,11));r.setFechaFin(LocalDate.of(2026,10,12));assertTrue(CalculadorDescuentosProductos.vigente(r,LocalDate.of(2026,10,12)));assertFalse(CalculadorDescuentosProductos.vigente(r,LocalDate.of(2026,10,13)));}
}
