package com.techprotech.agenda.modulos.productos.aplicacion;

import com.techprotech.agenda.modulos.productos.api.dto.ReglaComisionRequest;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio.PrestadorServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import com.techprotech.agenda.modulos.parametros.aplicacion.ServicioParametrosSistema;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class ServicioComisionesValidacionTest {
    private final ReglaComisionRepositorio reglas=mock(ReglaComisionRepositorio.class);
    private final ServicioComisiones servicio=new ServicioComisiones(reglas,mock(ServicioRepositorio.class),mock(ProductoRepositorio.class),mock(CategoriaProductoRepositorio.class),mock(PrestadorServicioRepositorio.class),mock(SucursalRepositorio.class),mock(ServicioParametrosSistema.class));
    private ReglaComisionRequest solicitud(String concepto,String alcance,Long producto,Long categoria) {
        return new ReglaComisionRequest("Prueba",null,concepto,alcance,null,producto,categoria,"PRESTADOR",null,"PORCENTAJE","NETO_DESCUENTOS",new BigDecimal("5"),null,null,null,0,true);
    }
    @Test void ventaTotalNoAceptaAlcanceEspecifico(){
        assertThatThrownBy(()->servicio.crear(9L,solicitud("VENTA_TOTAL","ESPECIFICO",null,null))).hasMessageContaining("alcance general");
        verifyNoInteractions(reglas);
    }
    @Test void servicioNoAceptaCategoriaProducto(){
        assertThatThrownBy(()->servicio.crear(9L,solicitud("SERVICIO","CATEGORIA",null,3L))).hasMessageContaining("sólo a productos");
        verifyNoInteractions(reglas);
    }
    @Test void productoEspecificoRequiereReferencia(){
        assertThatThrownBy(()->servicio.crear(9L,solicitud("PRODUCTO","ESPECIFICO",null,null))).hasMessageContaining("elemento");
        verifyNoInteractions(reglas);
    }
    @Test void categoriaRequiereReferencia(){
        assertThatThrownBy(()->servicio.crear(9L,solicitud("PRODUCTO","CATEGORIA",null,null))).hasMessageContaining("categoría");
        verifyNoInteractions(reglas);
    }
}
