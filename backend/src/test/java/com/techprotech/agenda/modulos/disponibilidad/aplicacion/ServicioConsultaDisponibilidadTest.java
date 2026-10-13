package com.techprotech.agenda.modulos.disponibilidad.aplicacion;

import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.disponibilidad.infraestructura.entidad.ReglaDisponibilidadEntidad;
import com.techprotech.agenda.modulos.disponibilidad.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.prestadores.infraestructura.entidad.PrestadorServicioEntidad;
import com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio.PrestadorServicioRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServicioConsultaDisponibilidadTest {
    @Test void rangoDe31DiasTresServiciosCincoPrestadoresPrecargaUnaVez() {
        configurarCatalogo();
        List<PrestadorServicioEntidad> personas=new ArrayList<>();
        List<ReglaDisponibilidadEntidad> precargadas=new ArrayList<>();
        for (long id=8;id<13;id++) {var persona=new PrestadorServicioEntidad();persona.setUsuarioId(id);personas.add(persona);}
        when(prestadores.findPublicosPorEmpresaYSucursal(42L,9L)).thenReturn(personas);
        for (long servicioId=2;servicioId<5;servicioId++) {
            var servicio=new ServicioEntidad();servicio.setId(servicioId);servicio.setDuracionMinutos(30);
            when(servicios.findByIdAndEmpresaIdAndActivoTrue(servicioId,42L)).thenReturn(Optional.of(servicio));
            when(asignacionSucursal.existsByEmpresaIdAndIdServicioIdAndIdSucursalIdAndActivoTrue(42L,servicioId,9L)).thenReturn(true);
            List<AsignacionServicioPrestadorEntidad> ofertas=new ArrayList<>();
            for (var persona:personas) {var oferta=new AsignacionServicioPrestadorEntidad();oferta.setId(new AsignacionServicioPrestadorId(persona.getUsuarioId(),servicioId));ofertas.add(oferta);}
            when(asignaciones.findActivasPorEmpresaYServicio(42L,servicioId)).thenReturn(ofertas);
        }
        for (int dia=1;dia<=7;dia++) {
            var sede=regla(9,18);sede.setDiaSemana(dia);sede.setTipoSujeto("SUCURSAL");sede.setSujetoId(9L);precargadas.add(sede);
            for (var persona:personas) {var regla=regla(9,18);regla.setDiaSemana(dia);regla.setTipoSujeto("PRESTADOR");regla.setSujetoId(persona.getUsuarioId());precargadas.add(regla);}
        }
        when(reglas.buscarParaRango(eq(42L),eq(9L),any())).thenReturn(precargadas);
        var resultado=consulta.obtenerDias(42L,9L,List.of(2L,3L,4L),null,fecha,fecha.plusDays(30));
        assertEquals(31,resultado.dias().size());
        verify(reglas).buscarParaRango(eq(42L),eq(9L),any());
        verify(excepciones).buscarParaRango(eq(42L),eq(9L),any(),eq(fecha),eq(fecha.plusDays(30)));
        verify(citas).buscarOcupadasRango(eq(42L),any(),any(),any(),any());
        verify(configuracion,times(1)).obtener(42L);
        verify(prestadores,times(1)).findPublicosPorEmpresaYSucursal(42L,9L);
        verify(reglas,never()).findByEmpresaIdAndTipoSujetoAndSujetoIdAndDiaSemana(any(),any(),any(),anyInt());
        verify(citas,never()).buscarConflictos(any(),any(),any(),any());
    }
    private final SucursalRepositorio sucursales = mock(SucursalRepositorio.class);
    private final ServicioRepositorio servicios = mock(ServicioRepositorio.class);
    private final ServicioSucursalRepositorio asignacionSucursal = mock(ServicioSucursalRepositorio.class);
    private final PrestadorServicioRepositorio prestadores = mock(PrestadorServicioRepositorio.class);
    private final AsignacionServicioPrestadorRepositorio asignaciones = mock(AsignacionServicioPrestadorRepositorio.class);
    private final ReglaDisponibilidadRepositorio reglas = mock(ReglaDisponibilidadRepositorio.class);
    private final ExcepcionDisponibilidadRepositorio excepciones = mock(ExcepcionDisponibilidadRepositorio.class);
    private final CitaRepositorio citas = mock(CitaRepositorio.class);
    private final ServicioConfiguracionReservas configuracion=mock(ServicioConfiguracionReservas.class);
    private final java.time.Clock reloj=java.time.Clock.fixed(Instant.parse("2026-10-12T15:00:00Z"),ZoneOffset.UTC);
    private final ServicioConsultaDisponibilidad consulta = new ServicioConsultaDisponibilidad(
            sucursales, servicios, asignacionSucursal, prestadores, asignaciones, reglas, excepciones, citas,configuracion,reloj);
    private final LocalDate fecha = LocalDate.of(2026, 10, 12);

    @Test void nuncaUsaUnTenantPorDefecto() {
        assertThrows(ResponseStatusException.class, () -> consulta.obtenerFranjasDisponibles(null, 9L, 2L, 8L, fecha));
        verifyNoInteractions(sucursales, servicios, reglas);
    }

    @Test void unServicioSinAsignacionALaSucursalNoOfreceHorarios() {
        configurarCatalogo();
        assertThrows(ResponseStatusException.class, () -> consulta.obtenerFranjasDisponibles(42L, 9L, 2L, 8L, fecha));
        verifyNoInteractions(prestadores, reglas);
    }

    @Test void soloOfreceInterseccionDeSucursalYPrestadorRespetandoDuracion() {
        configurarCatalogo();
        when(asignacionSucursal.existsByEmpresaIdAndIdServicioIdAndIdSucursalIdAndActivoTrue(42L, 2L, 9L)).thenReturn(true);
        var prestador = new PrestadorServicioEntidad(); prestador.setUsuarioId(8L);
        when(prestadores.findPublicosPorEmpresaYSucursal(42L,9L)).thenReturn(List.of(prestador));
        var asignacion = new AsignacionServicioPrestadorEntidad(); asignacion.setId(new AsignacionServicioPrestadorId(8L,2L));
        when(asignaciones.findActivasPorEmpresaYServicio(42L,2L)).thenReturn(List.of(asignacion));
        when(reglas.findByEmpresaIdAndTipoSujetoAndSujetoIdAndDiaSemana(42L, "SUCURSAL", 9L, 1))
                .thenReturn(List.of(regla(9, 18)));
        when(reglas.findByEmpresaIdAndTipoSujetoAndSujetoIdAndDiaSemana(42L, "PRESTADOR", 8L, 1))
                .thenReturn(List.of(regla(10, 12)));
        var franjas = consulta.obtenerFranjasDisponibles(42L, 9L, 2L, 8L, fecha);
        assertEquals(4, franjas.size());
        assertEquals(LocalTime.of(10, 0), OffsetDateTime.parse(franjas.getFirst().inicio()).toLocalTime());
        assertEquals(LocalTime.of(12, 0), OffsetDateTime.parse(franjas.getLast().fin()).toLocalTime());
    }

@org.junit.jupiter.params.ParameterizedTest
@org.junit.jupiter.params.provider.CsvSource({"2026-10-13T02:59:00Z,none", "2026-10-12T18:10:00Z,12:30", "2026-10-12T20:00:00Z,14:00"})
void elCorteRespetaHoraLocalYConservaFranjasFuturas(String ahora,String primera) {
    configurarCatalogo();
    when(asignacionSucursal.existsByEmpresaIdAndIdServicioIdAndIdSucursalIdAndActivoTrue(42L,2L,9L)).thenReturn(true);
    var prestador=new PrestadorServicioEntidad(); prestador.setUsuarioId(8L);
    when(prestadores.findPublicosPorEmpresaYSucursal(42L,9L)).thenReturn(List.of(prestador));
    var asignacion=new AsignacionServicioPrestadorEntidad(); asignacion.setId(new AsignacionServicioPrestadorId(8L,2L));
    when(asignaciones.findActivasPorEmpresaYServicio(42L,2L)).thenReturn(List.of(asignacion));
    when(reglas.findByEmpresaIdAndTipoSujetoAndSujetoIdAndDiaSemana(42L,"SUCURSAL",9L,1)).thenReturn(List.of(regla(9,18)));
    when(reglas.findByEmpresaIdAndTipoSujetoAndSujetoIdAndDiaSemana(42L,"PRESTADOR",8L,1)).thenReturn(List.of(regla(9,18)));
    var consultaAhora=new ServicioConsultaDisponibilidad(sucursales,servicios,asignacionSucursal,prestadores,asignaciones,reglas,excepciones,citas,configuracion,Clock.fixed(Instant.parse(ahora),ZoneOffset.UTC));
    var resultado=consultaAhora.obtenerFranjasDisponibles(42L,9L,2L,8L,fecha);
    if(primera.equals("none")) assertTrue(resultado.isEmpty());
    else assertEquals(LocalTime.parse(primera),OffsetDateTime.parse(resultado.getFirst().inicio()).toLocalTime());
}
@Test void fechasPasadasNoOfrecenFranjas() {
    configurarCatalogo(); when(asignacionSucursal.existsByEmpresaIdAndIdServicioIdAndIdSucursalIdAndActivoTrue(42L,2L,9L)).thenReturn(true);
    assertTrue(consulta.obtenerFranjasDisponibles(42L,9L,2L,8L,fecha.minusDays(1)).isEmpty());
    verifyNoInteractions(prestadores,reglas);
}
    private void configurarCatalogo() {
        when(configuracion.obtener(42L)).thenReturn(new com.techprotech.agenda.modulos.disponibilidad.api.dto.ConfiguracionReservasResponse(0,30,2,15));
        var sucursal = new SucursalEntidad(); sucursal.setId(9L); sucursal.setZonaHoraria("America/Mexico_City");
        when(sucursales.findByIdAndEmpresaIdAndActivaTrue(9L, 42L)).thenReturn(Optional.of(sucursal));
        var servicio = new ServicioEntidad(); servicio.setId(2L); servicio.setDuracionMinutos(30);
        when(servicios.findByIdAndEmpresaIdAndActivoTrue(2L, 42L)).thenReturn(Optional.of(servicio));
    }

    private ReglaDisponibilidadEntidad regla(int inicio, int fin) {
        var regla = new ReglaDisponibilidadEntidad(); regla.setHoraInicio(LocalTime.of(inicio, 0));
        regla.setHoraFin(LocalTime.of(fin, 0)); regla.setIntervaloMinutos(30); regla.setDiaSemana(1);
        return regla;
    }
}
