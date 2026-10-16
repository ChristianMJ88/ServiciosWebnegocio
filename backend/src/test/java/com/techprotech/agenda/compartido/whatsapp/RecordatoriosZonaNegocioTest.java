package com.techprotech.agenda.compartido.whatsapp;

import com.techprotech.agenda.compartido.correo.ServicioOutboxCorreoCitas;
import com.techprotech.agenda.compartido.tiempo.ZonaNegocio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.EmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Isolated("Zona JVM temporal para regresión de recordatorios")
@ExtendWith(MockitoExtension.class)
class RecordatoriosZonaNegocioTest {
    @Mock EmpresaRepositorio empresas;
    @Mock SucursalRepositorio sucursales;
    @Mock CitaRepositorio citas;
    @Mock ServicioOutboxCorreoCitas correo;
    @Mock ServicioOutboxWhatsappCitas whatsapp;
    @Mock Clock reloj;
    @Mock ZonaNegocio zonas;
    ProgramadorRecordatoriosWhatsapp programador;
    TimeZone anterior;

    @BeforeEach void preparar() {
        anterior = TimeZone.getDefault(); TimeZone.setDefault(TimeZone.getTimeZone("America/Mexico_City"));
        when(reloj.getZone()).thenReturn(ZoneOffset.UTC);
        when(reloj.instant()).thenReturn(Instant.parse("2026-10-14T16:00:00Z"));
        var empresa = new EmpresaEntidad(); empresa.setId(7L);
        var mexico = new SucursalEntidad(); mexico.setId(3L); mexico.setEmpresaId(7L);
        var bogota = new SucursalEntidad(); bogota.setId(4L); bogota.setEmpresaId(7L);
        when(empresas.findAll()).thenReturn(List.of(empresa));
        when(sucursales.findByEmpresaIdOrderByNombreAsc(7L)).thenReturn(List.of(mexico, bogota));
        when(zonas.de(empresa,mexico)).thenReturn(ZoneId.of("America/Mexico_City"));
        when(zonas.de(empresa,bogota)).thenReturn(ZoneId.of("America/Bogota"));
        var propiedades = new PropiedadesWhatsapp(false, false,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                20, 60, 24, 5, 2);
        programador = new ProgramadorRecordatoriosWhatsapp(citas, null, null, whatsapp, propiedades,
                null, null, correo, empresas, null, sucursales, reloj, zonas);
        when(citas.findByEmpresaIdAndSucursalIdAndInicioBetweenAndEstadoInOrderByInicioAsc(anyLong(),anyLong(),any(),any(),any()))
                .thenAnswer(i -> {
                    var inicio = LocalDateTime.parse("2026-10-15T10:00");
                    LocalDateTime desde = i.getArgument(2), hasta = i.getArgument(3);
                    if (i.getArgument(1).equals(3L) && !inicio.isBefore(desde) && !inicio.isAfter(hasta)) {
                        var cita = new CitaEntidad(); cita.setId(9L); cita.setEstado("CONFIRMADA"); cita.setInicio(inicio);
                        return List.of(cita);
                    }
                    return List.of();
                });
    }
    @AfterEach void restaurar() { TimeZone.setDefault(anterior); }

    @Test void recordatorio24HorasAntesSeBuscaALas10LocalesEnCadaSucursal() {
        when(reloj.withZone(any())).thenAnswer(i -> Clock.fixed(Instant.parse("2026-10-14T16:00:00Z"),i.getArgument(0)));
        programador.programarPendientes();
        verify(citas).findByEmpresaIdAndSucursalIdAndInicioBetweenAndEstadoInOrderByInicioAsc(7L,3L,
                LocalDateTime.parse("2026-10-15T10:00"),LocalDateTime.parse("2026-10-15T10:05"),List.of("PENDIENTE","CONFIRMADA"));
        verify(citas).findByEmpresaIdAndSucursalIdAndInicioBetweenAndEstadoInOrderByInicioAsc(7L,4L,
                LocalDateTime.parse("2026-10-15T11:00"),LocalDateTime.parse("2026-10-15T11:05"),List.of("PENDIENTE","CONFIRMADA"));
        verify(correo).programarRecordatorio(7L,9L,LocalDateTime.parse("2026-10-15T10:00"));
    }

    @Test void noProgramaSeisHorasAntesDeLaVentana() {
        when(reloj.withZone(any())).thenAnswer(i -> Clock.fixed(Instant.parse("2026-10-14T10:00:00Z"),i.getArgument(0)));
        programador.programarPendientes();
        verifyNoInteractions(correo, whatsapp);
    }
}
