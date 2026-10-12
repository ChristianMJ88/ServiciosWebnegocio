package com.techprotech.agenda.modulos.ventasstaff.aplicacion;

import com.techprotech.agenda.modulos.ventasstaff.infraestructura.VentasStaffRepositorio;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ServicioVentasStaffTest {
    @Test void limitaConsultaAlTenantYColaboradorAutenticados() {
        var repo=mock(VentasStaffRepositorio.class);
        var servicio=new ServicioVentasStaff(repo);
        var fecha=LocalDate.of(2026,10,11);
        when(repo.consultar(anyLong(),anyLong(),any(),any(),anyString(),anyString(),any())).thenReturn(org.springframework.data.domain.Page.empty());
        servicio.consultar(42L,97L,fecha,fecha,"producto"," Crema ",2,20);
        verify(repo).consultar(42L,97L,fecha.atStartOfDay(),fecha.plusDays(1).atStartOfDay(),"PRODUCTO","crema",PageRequest.of(2,20));
    }
    @Test void rechazaRangoInvalidoSinConsultarDatos() {
        var repo=mock(VentasStaffRepositorio.class);
        var servicio=new ServicioVentasStaff(repo);
        assertThrows(ResponseStatusException.class,()->servicio.consultar(42L,97L,LocalDate.of(2026,10,12),LocalDate.of(2026,10,11),"","",0,20));
        verifyNoInteractions(repo);
    }
}
