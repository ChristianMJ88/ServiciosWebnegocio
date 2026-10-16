package com.techprotech.agenda.modulos.ventasstaff.aplicacion;

import com.techprotech.agenda.modulos.ventasstaff.infraestructura.VentasStaffRepositorio;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ServicioVentasStaffTest {
    @Test void rangoPredeterminadoEsCalendarioDelNegocioConvertidoAUtcParaLaVenta() {
        var repo=mock(VentasStaffRepositorio.class);
        var zona=java.time.ZoneId.of("America/Mexico_City");
        var reloj=java.time.Clock.fixed(java.time.Instant.parse("2026-10-14T01:00:00Z"),java.time.ZoneOffset.UTC);
        var servicio=new ServicioVentasStaff(repo,reloj,com.techprotech.agenda.compartido.tiempo.TiempoPruebas.zona(zona));
        when(repo.consultar(anyLong(),anyLong(),any(),any(),anyString(),anyString(),any())).thenReturn(org.springframework.data.domain.Page.empty());
        servicio.consultar(42L,97L,null,null,null,null,0,20);
        verify(repo).consultar(42L,97L,java.time.LocalDateTime.parse("2026-10-01T06:00"),
            java.time.LocalDateTime.parse("2026-10-14T06:00"),"","",PageRequest.of(0,20));
    }
    @Test void limitaConsultaAlTenantYColaboradorAutenticados() {
        var repo=mock(VentasStaffRepositorio.class);
        var servicio=new ServicioVentasStaff(repo, java.time.Clock.systemUTC(), com.techprotech.agenda.compartido.tiempo.TiempoPruebas.zonaUtc());
        var fecha=LocalDate.of(2026,10,11);
        when(repo.consultar(anyLong(),anyLong(),any(),any(),anyString(),anyString(),any())).thenReturn(org.springframework.data.domain.Page.empty());
        servicio.consultar(42L,97L,fecha,fecha,"producto"," Crema ",2,20);
        verify(repo).consultar(42L,97L,fecha.atStartOfDay(),fecha.plusDays(1).atStartOfDay(),"PRODUCTO","crema",PageRequest.of(2,20));
    }
    @Test void rechazaRangoInvalidoSinConsultarDatos() {
        var repo=mock(VentasStaffRepositorio.class);
        var servicio=new ServicioVentasStaff(repo, java.time.Clock.systemUTC(), com.techprotech.agenda.compartido.tiempo.TiempoPruebas.zonaUtc());
        assertThrows(ResponseStatusException.class,()->servicio.consultar(42L,97L,LocalDate.of(2026,10,12),LocalDate.of(2026,10,11),"","",0,20));
        verifyNoInteractions(repo);
    }
}
