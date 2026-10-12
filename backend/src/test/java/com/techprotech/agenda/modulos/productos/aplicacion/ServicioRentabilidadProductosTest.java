package com.techprotech.agenda.modulos.productos.aplicacion;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
class ServicioRentabilidadProductosTest {
 @Test void costoAusenteNoEsCero(){assertNull(ServicioRentabilidadProductos.margen(new BigDecimal("80"), null));}
 @Test void precioCeroNoDivide(){assertNull(ServicioRentabilidadProductos.margen(BigDecimal.ZERO, new BigDecimal("10")));}
 @Test void margenNoEsRecargoSobreCosto(){assertEquals(new BigDecimal("25.00"),ServicioRentabilidadProductos.margen(new BigDecimal("80"),new BigDecimal("60")));}
 @Test void perdidasConservanSigno(){assertEquals(new BigDecimal("-25.00"),ServicioRentabilidadProductos.margen(new BigDecimal("80"),new BigDecimal("100")));}
}
