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
    private final SucursalRepositorio sucursales = mock(SucursalRepositorio.class);
    private final ServicioRepositorio servicios = mock(ServicioRepositorio.class);
    private final ServicioSucursalRepositorio asignacionSucursal = mock(ServicioSucursalRepositorio.class);
    private final PrestadorServicioRepositorio prestadores = mock(PrestadorServicioRepositorio.class);
    private final AsignacionServicioPrestadorRepositorio asignaciones = mock(AsignacionServicioPrestadorRepositorio.class);
    private final ReglaDisponibilidadRepositorio reglas = mock(ReglaDisponibilidadRepositorio.class);
    private final ExcepcionDisponibilidadRepositorio excepciones = mock(ExcepcionDisponibilidadRepositorio.class);
    private final CitaRepositorio citas = mock(CitaRepositorio.class);
    private final ServicioConsultaDisponibilidad consulta = new ServicioConsultaDisponibilidad(
            sucursales, servicios, asignacionSucursal, prestadores, asignaciones, reglas, excepciones, citas);
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
        when(prestadores.findByUsuarioIdAndSucursalIdAndActivoTrue(8L, 9L)).thenReturn(Optional.of(prestador));
        when(asignaciones.findByIdPrestadorIdAndIdServicioIdAndActivaTrue(8L, 2L))
                .thenReturn(Optional.of(new AsignacionServicioPrestadorEntidad()));
        when(reglas.findByEmpresaIdAndTipoSujetoAndSujetoIdAndDiaSemana(42L, "SUCURSAL", 9L, 1))
                .thenReturn(List.of(regla(9, 18)));
        when(reglas.findByEmpresaIdAndTipoSujetoAndSujetoIdAndDiaSemana(42L, "PRESTADOR", 8L, 1))
                .thenReturn(List.of(regla(10, 12)));
        var franjas = consulta.obtenerFranjasDisponibles(42L, 9L, 2L, 8L, fecha);
        assertEquals(4, franjas.size());
        assertEquals(LocalTime.of(10, 0), OffsetDateTime.parse(franjas.getFirst().inicio()).toLocalTime());
        assertEquals(LocalTime.of(12, 0), OffsetDateTime.parse(franjas.getLast().fin()).toLocalTime());
    }

    private void configurarCatalogo() {
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
