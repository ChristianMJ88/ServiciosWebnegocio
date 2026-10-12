package com.techprotech.agenda.modulos.admin.aplicacion;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.prestadores.infraestructura.entidad.PrestadorServicioEntidad;
import com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio.PrestadorServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServicioImpactoPrestadorTest {
    final UsuarioRepositorio usuarios = mock(UsuarioRepositorio.class);
    final PrestadorServicioRepositorio prestadores = mock(PrestadorServicioRepositorio.class);
    final SucursalRepositorio sucursales = mock(SucursalRepositorio.class);
    final CitaRepositorio citas = mock(CitaRepositorio.class);
    final ServicioImpactoPrestador servicio = new ServicioImpactoPrestador(usuarios,prestadores,citas,sucursales);
    @Test void noConsultaAgendaDeOtroTenant() {
        assertThrows(ResponseStatusException.class,()->servicio.consultar(10L,2L));
        verifyNoInteractions(prestadores,sucursales,citas);
    }
    @Test void devuelveEstadosDelBackendSinModificarCitas() {
        when(usuarios.findByIdAndEmpresaId(2L,10L)).thenReturn(Optional.of(new UsuarioEntidad()));
        var prestador = new PrestadorServicioEntidad(); prestador.setSucursalId(3L);
        when(prestadores.findById(2L)).thenReturn(Optional.of(prestador));
        var sucursal = new SucursalEntidad(); sucursal.setZonaHoraria("America/Mexico_City");
        when(sucursales.findByIdAndEmpresaId(3L,10L)).thenReturn(Optional.of(sucursal));
        var estado = mock(CitaRepositorio.ConteoEstadoPrestador.class);
        when(estado.getEstado()).thenReturn("ESTADO_DEL_CATALOGO"); when(estado.getCantidad()).thenReturn(4L);
        when(citas.contarCitasFuturasPorEstado(eq(10L),eq(2L),any())).thenReturn(List.of(estado));
        var resultado = servicio.consultar(10L,2L);
        assertEquals("ESTADO_DEL_CATALOGO",resultado.citasFuturas().getFirst().estado());
        assertEquals(4L,resultado.citasFuturas().getFirst().cantidad()); verify(citas,never()).save(any());
    }
}
