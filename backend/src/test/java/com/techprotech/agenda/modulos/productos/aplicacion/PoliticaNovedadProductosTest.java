package com.techprotech.agenda.modulos.productos.aplicacion;
import org.junit.jupiter.api.Test;
import java.time.*;
import com.techprotech.agenda.modulos.productos.infraestructura.entidad.ProductoEntidad;
import static org.junit.jupiter.api.Assertions.*;
class PoliticaNovedadProductosTest {
 @Test void duracionConfigurableNoSeReiniciaAlEditar(){var p=new ProductoEntidad();p.setCreadoEn(LocalDateTime.of(2026,10,1,10,0));assertTrue(PoliticaNovedadProductos.esNuevo(p,7,LocalDate.of(2026,10,7)));assertFalse(PoliticaNovedadProductos.esNuevo(p,7,LocalDate.of(2026,10,8)));assertFalse(PoliticaNovedadProductos.esNuevo(p,0,LocalDate.of(2026,10,1)));}
 @Test void vigenciaIndividualReemplazaAutomatica(){var p=new ProductoEntidad();p.setCreadoEn(LocalDateTime.of(2026,10,1,10,0));p.setNuevoDesde(LocalDate.of(2026,11,1));p.setNuevoHasta(LocalDate.of(2026,11,3));assertFalse(PoliticaNovedadProductos.esNuevo(p,100,LocalDate.of(2026,10,2)));assertTrue(PoliticaNovedadProductos.esNuevo(p,0,LocalDate.of(2026,11,3)));assertFalse(PoliticaNovedadProductos.esNuevo(p,100,LocalDate.of(2026,11,4)));}
}
