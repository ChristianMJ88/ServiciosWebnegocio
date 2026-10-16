package com.techprotech.agenda.modulos.productos.aplicacion;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.RentabilidadVentasRepositorio;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ServicioUtilidadRealizadaTest {
    private RentabilidadVentasRepositorio.Fila fila(long faltantes){
        var f=mock(RentabilidadVentasRepositorio.Fila.class);
        when(f.getLineasSinCosto()).thenReturn(faltantes);
        when(f.getVentasNetas()).thenReturn(new BigDecimal("370.00"));
        when(f.getCostoConocido()).thenReturn(new BigDecimal("200.00"));
        return f;
    }
    @Test void calculaCostoAcumuladoYMargenSobreVentas(){
        var r=ServicioUtilidadRealizada.mapear(fila(0));
        assertEquals(new BigDecimal("170.00"),r.utilidadBruta());
        assertEquals(new BigDecimal("45.95"),r.margenPorcentaje());
    }
    @Test void noPresentaUtilidadParcialComoCompleta(){
        var r=ServicioUtilidadRealizada.mapear(fila(1));
        assertNull(r.costoHistorico());assertNull(r.utilidadBruta());assertNull(r.margenPorcentaje());
    }
    @Test void consultaTenantPeriodoInclusivoYPaginaEnRepositorio(){
        var repo=mock(RentabilidadVentasRepositorio.class);
        var desde=LocalDate.of(2026,10,1);var hasta=LocalDate.of(2026,10,11);
        when(repo.consultar(77L,desde.atStartOfDay(),hasta.plusDays(1).atStartOfDay(),3L,"crema",PageRequest.of(0,25)))
            .thenReturn(new PageImpl<>(List.of()));
        new ServicioUtilidadRealizada(repo, com.techprotech.agenda.compartido.tiempo.TiempoPruebas.zonaUtc()).consultar(77L,desde,hasta,3L," Crema ",0);
        verify(repo).consultar(77L,desde.atStartOfDay(),hasta.plusDays(1).atStartOfDay(),3L,"crema",PageRequest.of(0,25));
    }
    @Test void rechazaPeriodoInvertidoAntesDeLeerVentas(){
        var repo=mock(RentabilidadVentasRepositorio.class);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->new ServicioUtilidadRealizada(repo, com.techprotech.agenda.compartido.tiempo.TiempoPruebas.zonaUtc())
            .consultar(77L,LocalDate.of(2026,10,11),LocalDate.of(2026,10,1),null,"",0));
        verifyNoInteractions(repo);
    }
}
