package com.techprotech.agenda.modulos.clientes.aplicacion;

import com.techprotech.agenda.modulos.clientes.infraestructura.ServicioClientesJpa;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ServicioClientesFiltrosTest {
    @Test void pasaTenantYFiltrosAlRepositorioSinConsultarClientesUnoPorUno() {
        var repo=mock(ClienteRepositorio.class);
        when(repo.buscarResumenes(eq(77L),anyString(),anyString(),anyString(),isNull(),isNull(),anyString(),anyString(),any(),any())).thenReturn(Page.empty());
        var servicio=new ServicioClientesJpa(repo,null,null,null,null, java.time.Clock.systemUTC(), com.techprotech.agenda.compartido.tiempo.TiempoPruebas.zonaUtc());
        servicio.listar(77L,new FiltroClientes(null,"Ana","222",null,null,FiltroClientes.Citas.CON_AGENDADAS,FiltroClientes.Orden.ULTIMA_VISITA_DESC),PageRequest.of(0,25));
        verify(repo).buscarResumenes(eq(77L),eq(""),eq("ana"),eq("222"),isNull(),isNull(),eq("CON_AGENDADAS"),eq("ULTIMA_VISITA_DESC"),any(),eq(PageRequest.of(0,25)));
        verifyNoMoreInteractions(repo);
    }
}
