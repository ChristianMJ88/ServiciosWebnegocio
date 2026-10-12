package com.techprotech.agenda.modulos.disponibilidad.aplicacion;

import com.techprotech.agenda.modulos.disponibilidad.api.dto.*;
import com.techprotech.agenda.modulos.disponibilidad.infraestructura.entidad.ReglaDisponibilidadEntidad;
import com.techprotech.agenda.modulos.disponibilidad.infraestructura.repositorio.ReglaDisponibilidadRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.LocalTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServicioHorarioSemanalTest {
    private final ServicioGestionDisponibilidad gestion = mock(ServicioGestionDisponibilidad.class);
    private final ReglaDisponibilidadRepositorio repo = mock(ReglaDisponibilidadRepositorio.class);
    private final ServicioHorarioSemanalImpl servicio = new ServicioHorarioSemanalImpl(gestion, repo);
    private final ReglaDisponibilidadRequest horario = new ReglaDisponibilidadRequest(
            "SUCURSAL", 9L, 1, LocalTime.of(9, 0), LocalTime.of(18, 0), 30, null, null);

    @BeforeEach void configurar() {
        when(gestion.validarReglaAdmin(42L, horario)).thenReturn("SUCURSAL");
        when(repo.findByEmpresaIdAndTipoSujetoAndSujetoIdOrderByDiaSemanaAscHoraInicioAsc(42L, "SUCURSAL", 9L))
                .thenReturn(List.of());
    }

    @Test void aplicaDiasSinDuplicarlosDentroDelTenant() {
        servicio.guardar(42L, new HorarioSemanalRequest(horario, List.of(3, 1, 3), false));
        verify(gestion).crearReglaAdmin(42L, horario);
        verify(gestion).crearReglaAdmin(42L, new ReglaDisponibilidadRequest(
                "SUCURSAL", 9L, 3, horario.horaInicio(), horario.horaFin(), 30, null, null));
        verify(gestion, times(2)).crearReglaAdmin(eq(42L), any());
    }

    @Test void staffNoPuedeCambiarLaIdentidadDelHorarioEnElCuerpo() {
        var propio = new ReglaDisponibilidadRequest("PRESTADOR", 8L, 1,
                horario.horaInicio(), horario.horaFin(), 30, null, null);
        when(gestion.validarReglaAdmin(42L, propio)).thenReturn("PRESTADOR");
        when(repo.findByEmpresaIdAndTipoSujetoAndSujetoIdOrderByDiaSemanaAscHoraInicioAsc(42L, "PRESTADOR", 8L))
                .thenReturn(List.of());
        servicio.guardarStaff(42L, 8L, new HorarioSemanalRequest(horario, List.of(1), false));
        verify(gestion).validarReglaAdmin(42L, propio);
        verify(gestion).crearReglaAdmin(42L, propio);
        verify(gestion, never()).crearReglaAdmin(42L, horario);
        verify(repo, never()).findByEmpresaIdAndTipoSujetoAndSujetoIdOrderByDiaSemanaAscHoraInicioAsc(42L, "SUCURSAL", 9L);
    }

    @Test void validaPertenenciaAntesDeConsultarOModificar() {
        when(gestion.validarReglaAdmin(42L, horario)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        assertThrows(ResponseStatusException.class, () -> servicio.guardar(42L,
                new HorarioSemanalRequest(horario, List.of(1), true)));
        verifyNoInteractions(repo);
        verify(gestion, never()).crearReglaAdmin(anyLong(), any());
    }

    @Test void omiteUnaReglaExactaYaGuardada() {
        var existente = regla(1, 9, 18);
        when(repo.findByEmpresaIdAndTipoSujetoAndSujetoIdOrderByDiaSemanaAscHoraInicioAsc(42L, "SUCURSAL", 9L))
                .thenReturn(List.of(existente));
        servicio.guardar(42L, new HorarioSemanalRequest(horario, List.of(1), false));
        verify(gestion, never()).crearReglaAdmin(anyLong(), any());
    }

    @Test void unCruceRechazaTodaLaSemanaAntesDeEscribir() {
        when(repo.findByEmpresaIdAndTipoSujetoAndSujetoIdOrderByDiaSemanaAscHoraInicioAsc(42L, "SUCURSAL", 9L))
                .thenReturn(List.of(regla(3, 10, 14)));
        var error = assertThrows(ResponseStatusException.class, () -> servicio.guardar(42L,
                new HorarioSemanalRequest(horario, List.of(1, 3), false)));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repo, never()).deleteAll(any());
        verify(gestion, never()).crearReglaAdmin(anyLong(), any());
    }

    @Test void reemplazaSoloLosDiasSeleccionados() {
        var lunes = regla(1, 10, 14);
        var domingo = regla(7, 10, 14);
        when(repo.findByEmpresaIdAndTipoSujetoAndSujetoIdOrderByDiaSemanaAscHoraInicioAsc(42L, "SUCURSAL", 9L))
                .thenReturn(List.of(lunes, domingo));
        servicio.guardar(42L, new HorarioSemanalRequest(horario, List.of(1), true));
        verify(repo).deleteAll(List.of(lunes));
        verify(gestion).crearReglaAdmin(42L, horario);
    }

    private ReglaDisponibilidadEntidad regla(int dia, int apertura, int cierre) {
        var regla = new ReglaDisponibilidadEntidad();
        regla.setEmpresaId(42L); regla.setTipoSujeto("SUCURSAL"); regla.setSujetoId(9L);
        regla.setDiaSemana(dia); regla.setHoraInicio(LocalTime.of(apertura, 0));
        regla.setHoraFin(LocalTime.of(cierre, 0)); regla.setIntervaloMinutos(30);
        return regla;
    }
}
