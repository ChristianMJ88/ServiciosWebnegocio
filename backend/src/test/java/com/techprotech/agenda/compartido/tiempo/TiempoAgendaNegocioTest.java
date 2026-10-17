package com.techprotech.agenda.compartido.tiempo;

import com.techprotech.agenda.modulos.admin.aplicacion.ServicioAdminCitas;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.citas.aplicacion.ServicioCitasCliente;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.disponibilidad.aplicacion.ServicioConsultaDisponibilidad;
import com.techprotech.agenda.modulos.recepcion.aplicacion.ServicioRecepcion;
import com.techprotech.agenda.modulos.recepcion.infraestructura.repositorio.SolicitudEsperaRecepcionRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import com.techprotech.agenda.compartido.correo.ServicioOutboxCorreoCitas;
import java.math.BigDecimal;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import com.techprotech.agenda.modulos.caja.aplicacion.*;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Isolated("Comprueba reloj UTC con JVM America/Mexico_City y restaura su zona")
@ExtendWith(MockitoExtension.class)
class TiempoAgendaNegocioTest {
    @Mock PagoCitaRepositorio pagos;
    @Mock VentaDetalleRepositorio detalles;
    @Mock Clock reloj;
    @Mock ZonaNegocio zonas;
    @Mock CitaRepositorio citas;
    @Mock SucursalRepositorio sucursales;
    @Mock HistorialEstadoCitaRepositorio historial;
    @Mock ClienteRepositorio clientes;
    @Mock com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio servicios;
    @Mock UsuarioRepositorio usuarios;
    @Mock com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio.PrestadorServicioRepositorio prestadores;
    @Mock SolicitudEsperaRecepcionRepositorio espera;
    @Mock ServicioConsultaDisponibilidad disponibilidad;
    @Mock ServicioOutboxCorreoCitas correo;
    @InjectMocks ServicioRecepcion recepcion;
    @InjectMocks ServicioAdminCitas admin;
    @Mock com.techprotech.agenda.modulos.admin.aplicacion.ServicioReporteCobroCitas reporteCobros;
    @InjectMocks ServicioCitasCliente cliente;
    @InjectMocks com.techprotech.agenda.modulos.prestadores.aplicacion.ServicioAgendaStaff staff;
    @Mock com.techprotech.agenda.modulos.caja.infraestructura.repositorio.CajaSesionRepositorio sesiones;
    @InjectMocks com.techprotech.agenda.modulos.caja.aplicacion.ServicioCaja caja;
    Instant ahora = Instant.parse("2026-10-14T01:00:00Z");
    TimeZone anterior;

    @BeforeEach void preparar() {
        var saldos = new ServicioSaldoCitas(citas,pagos,detalles);
        var cobros = new ServicioConsultaCobroCitas(saldos,sesiones,null);
        ReflectionTestUtils.setField(admin,"cobros",cobros);
        var cancelaciones = new com.techprotech.agenda.modulos.citas.aplicacion.ServicioCancelacionCita(citas,saldos);
        ReflectionTestUtils.setField(admin,"cancelaciones",cancelaciones);
        ReflectionTestUtils.setField(recepcion,"cancelaciones",cancelaciones);
        var cierres = new com.techprotech.agenda.modulos.citas.aplicacion.ServicioCierreCita(citas,historial,correo,null,clientes,reloj,zonas);
        ReflectionTestUtils.setField(recepcion,"cierres",cierres);
        ReflectionTestUtils.setField(staff,"cierres",cierres);
        ReflectionTestUtils.setField(recepcion,"cobros",cobros);
        ReflectionTestUtils.setField(staff,"cobros",cobros);
        ReflectionTestUtils.setField(caja,"saldos",saldos);
        anterior = TimeZone.getDefault(); TimeZone.setDefault(TimeZone.getTimeZone("America/Mexico_City"));
        lenient().when(reloj.withZone(any())).thenAnswer(i -> Clock.fixed(ahora, i.getArgument(0)));
        lenient().when(reloj.instant()).thenAnswer(i -> ahora);
        lenient().when(reloj.getZone()).thenReturn(ZoneOffset.UTC);
        lenient().when(zonas.de(anyLong(), nullable(Long.class))).thenReturn(ZoneId.of("America/Mexico_City"));
    }
    @AfterEach void restaurar() { TimeZone.setDefault(anterior); }

    @Test void recepcionConsultaEl13ALas19LocalesAunqueEnUtcYaSea14() {
        recepcion.agenda(actor(11L,List.of(3L)), null, 3L);
        verify(citas).findByEmpresaIdAndInicioBetweenOrderByInicioAsc(7L,
                LocalDateTime.parse("2026-10-13T00:00"), LocalDateTime.parse("2026-10-14T00:00"));
        recepcion.solicitudesEspera(7L, List.of(3L), null, 3L);
        verify(espera).findByEmpresaIdAndFechaDeseadaOrderByCreadaEnAsc(7L, LocalDate.of(2026,10,13));
        recepcion.franjasDisponibles(7L, List.of(3L), 3L, 8L, null, null);
        verify(disponibilidad).obtenerFranjasDisponibles(7L, 3L, 8L, null, LocalDate.of(2026,10,13));
    }

    @Test void fechaExplicitaDeRecepcionSeRespeta() {
        recepcion.agenda(actor(11L,List.of()), LocalDate.of(2026,11,2), null);
        verify(citas).findByEmpresaIdAndInicioBetweenOrderByInicioAsc(7L,
                LocalDate.of(2026,11,2).atStartOfDay(), LocalDate.of(2026,11,3).atStartOfDay());
    }

    @Test void agendaStaffParteDelHoyDeSuSucursal() {
        var prestador = new com.techprotech.agenda.modulos.prestadores.infraestructura.entidad.PrestadorServicioEntidad();
        prestador.setUsuarioId(9L); prestador.setSucursalId(3L);
        var sucursal = new com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad();
        sucursal.setId(3L); sucursal.setEmpresaId(7L);
        when(prestadores.findById(9L)).thenReturn(Optional.of(prestador));
        when(sucursales.findById(3L)).thenReturn(Optional.of(sucursal));
        staff.obtenerAgenda(actor(9L,List.of(3L)),null,null);
        verify(citas).findByEmpresaIdAndPrestadorIdAndInicioBetweenOrderByInicioAsc(7L,9L,
                LocalDateTime.parse("2026-10-13T00:00"),LocalDateTime.parse("2026-10-21T00:00"));
    }

    @Test void resumenUsaElHoyDeCadaSucursalYPeriodosElDeLaEmpresa() {
        var local = cita(3L, "2026-10-13T20:00");
        var utc = cita(4L, "2026-10-14T02:00");
        when(zonas.de(7L, 4L)).thenReturn(ZoneOffset.UTC);
        when(citas.findByEmpresaIdOrderByInicioDesc(7L)).thenReturn(List.of(local, utc));
        assertThat(admin.resumen(actor(11L,List.of()), null, null).citasHoy()).isEqualTo(2);
        assertThat(admin.periodosReporte(7L).stream().filter(p -> p.codigo().equals("ULTIMOS_30_DIAS")).findFirst().orElseThrow().hasta())
                .isEqualTo(LocalDate.of(2026,10,13));
    }

    @Test void cancelacionALas08LocalesSePermiteYAuditoriaSeGuardaEnUtc() {
        ahora = Instant.parse("2026-10-15T14:00:00Z");
        var cita = cita(3L, "2026-10-15T10:00");
        when(citas.findByIdAndEmpresaIdAndClienteId(9L,7L,10L)).thenReturn(Optional.of(cita));
        cliente.cancelar(7L,10L,9L);
        assertThat(cita.getEstado()).isEqualTo("CANCELADA");
        assertThat(cita.getCanceladaEn()).isEqualTo(LocalDateTime.parse("2026-10-15T14:00"));
        verify(correo).programarCanceladaCliente(7L,9L,cita.getInicio());
    }

    @Test void cancelacionDespuesDelInicioSeRechazaSinGuardarNiNotificar() {
        ahora = Instant.parse("2026-10-15T16:01:00Z");
        when(citas.findByIdAndEmpresaIdAndClienteId(9L,7L,10L)).thenReturn(Optional.of(cita(3L,"2026-10-15T10:00")));
        assertThatThrownBy(() -> cliente.cancelar(7L,10L,9L)).isInstanceOf(ResponseStatusException.class);
        verify(citas, never()).save(any()); verifyNoInteractions(correo, historial);
    }

    @Test void recepcionYAdminEscribenLosMismosMomentosUtcQueCliente() {
        ahora=Instant.parse("2026-10-15T14:00:00Z");
        var cita=cita(3L,"2026-10-15T10:00");
        when(citas.bloquearPorIdYEmpresa(9L,7L)).thenReturn(Optional.of(cita));
        when(citas.save(any())).thenAnswer(i -> i.getArgument(0));
        var respuesta=recepcion.checkIn(actor(11L,List.of(3L)),9L);
        assertThat(respuesta.checkInEn()).isEqualTo(OffsetDateTime.parse("2026-10-15T14:00Z"));
        assertThat(respuesta.inicio().getHour()).isEqualTo(10);
        assertThat(cita.getCheckInEn()).isEqualTo(LocalDateTime.parse("2026-10-15T14:00"));
        assertThat(cita.getInicio()).isEqualTo(LocalDateTime.parse("2026-10-15T10:00"));
        var llegada = cita.getCheckInEn();
        // La cancelación ya no acepta check-in; la auditoría UTC se comprueba en una cita cancelable.
        cita.setCheckInEn(null);
        recepcion.cancelar(actor(11L,List.of(3L)),9L);
        assertThat(cita.getCanceladaEn()).isEqualTo(llegada);
        cita.setEstado("CONFIRMADA");cita.setCanceladaEn(null);
        admin.cambiarEstadoCita(actor(11L,List.of(3L)),9L,"CANCELADA");
        assertThat(cita.getCanceladaEn()).isEqualTo(llegada);
    }

    @Test void aperturaDeCajaUsaUtcSinCambiarLaSucursalNiElImporte() {
        ahora=Instant.parse("2026-10-15T14:00:00Z");
        when(sesiones.save(any())).thenAnswer(i -> i.getArgument(0));
        var respuesta=caja.abrirSesion(7L,11L,List.of(3L),
                new com.techprotech.agenda.modulos.caja.api.dto.AbrirCajaRequest(3L,BigDecimal.TEN,null));
        assertThat(respuesta.abiertaEn()).isEqualTo(OffsetDateTime.parse("2026-10-15T14:00Z"));
        assertThat(respuesta.sucursalId()).isEqualTo(3L);
        assertThat(respuesta.montoInicial()).isEqualByComparingTo(BigDecimal.TEN);
    }

    @Test void listadoClienteResuelveLaZonaUnaVezPorSucursal() {
        var primera=cita(3L,"2026-10-15T10:00");var segunda=cita(3L,"2026-10-16T10:00");
        segunda.setId(10L);
        when(citas.findByEmpresaIdAndClienteIdOrderByInicioDesc(7L,10L)).thenReturn(List.of(primera,segunda));
        assertThat(cliente.listarMisCitas(7L,10L)).hasSize(2).allMatch(c -> c.cancelable());
        verify(zonas,times(1)).de(7L,3L);
    }

    static UsuarioAutenticado actor(Long id,List<Long> scope) {
        return new UsuarioAutenticado(null,id,7L,"EMPRESA",List.of(),List.of(),scope);
    }

    static CitaEntidad cita(Long sucursal, String inicio) {
        var c = new CitaEntidad(); c.setId(9L); c.setEmpresaId(7L); c.setSucursalId(sucursal);
        c.setClienteId(10L); c.setEstado("CONFIRMADA"); c.setPrecio(BigDecimal.TEN);
        c.setInicio(LocalDateTime.parse(inicio)); c.setFin(c.getInicio().plusHours(1)); return c;
    }
}
