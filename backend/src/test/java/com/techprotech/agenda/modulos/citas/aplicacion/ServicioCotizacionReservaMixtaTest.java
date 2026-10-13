package com.techprotech.agenda.modulos.citas.aplicacion;
import com.techprotech.agenda.modulos.citas.api.dto.*;
import com.techprotech.agenda.modulos.productos.aplicacion.ServicioCotizacionProductos;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class ServicioCotizacionReservaMixtaTest {
    final ServicioCotizacionCitas citas=mock(ServicioCotizacionCitas.class);
    final ServicioCotizacionProductos productos=mock(ServicioCotizacionProductos.class);
    final ServicioPoliticaReservaPago politica=mock(ServicioPoliticaReservaPago.class);
    final ServicioCotizacionReservaMixta servicio=new ServicioCotizacionReservaMixta(citas,productos,politica);
    @Test void sumaAnticipoYProductoConDescuentoSinCobrarServicioLibre() {
        comprobar(true,true,"400");
    }
    @Test void soloProductosNoFuerzaCobroDeCitasLibres() { comprobar(false,true,"200"); }
    @Test void productosNoDisponiblesOnlineSeConservanEnCotizacionConAviso() { comprobar(false,false,"0"); }
    void comprobar(boolean pagoCitas,boolean pagoProductos,String requerido) {
        var request=new CotizarCitasRequest(7L,3L,null,List.of(new CotizarCitasRequest.ItemCotizacion(8L,null)),List.of(new CotizarCitasRequest.Producto(9L,BigDecimal.ONE)));
        when(citas.cotizar(7L,3L,null,request.items())).thenReturn(new ResultadoCotizacionCitas("MXN",new BigDecimal("700"),BigDecimal.ZERO,new BigDecimal("700"),new BigDecimal(pagoCitas?"200":"0"),null,null,List.of(),pagoCitas,new BigDecimal(pagoCitas?"200":"0")));
        when(productos.cotizar(7L,3L,null,Map.of(9L,BigDecimal.ONE))).thenReturn(new ServicioCotizacionProductos.ResultadoCotizacion(null,List.of(new ServicioCotizacionProductos.CotizacionProducto(9L,"Producto",BigDecimal.ONE,new BigDecimal("250"),new BigDecimal("200"),new BigDecimal("50"),new BigDecimal("200"),"MXN"))));
        when(politica.puedeCobrarProductosOnline(7L)).thenReturn(pagoProductos);
        var resultado=servicio.cotizar(request);
        assertThat(resultado.total()).isEqualByComparingTo("900"); assertThat(resultado.descuentoTotal()).isEqualByComparingTo("50");
        assertThat(resultado.importeRequeridoTotal()).isEqualByComparingTo(requerido);
        assertThat(resultado.requierePagoProductos()).isEqualTo(pagoProductos);
        if(!pagoProductos) assertThat(resultado.avisoProductos()).isNotBlank();
        verify(citas).cotizar(7L,3L,null,request.items());
        verifyNoMoreInteractions(citas);
    }
}
