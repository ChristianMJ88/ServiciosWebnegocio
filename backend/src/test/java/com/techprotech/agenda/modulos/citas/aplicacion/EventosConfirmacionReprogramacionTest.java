package com.techprotech.agenda.modulos.citas.aplicacion;

import com.techprotech.agenda.compartido.tiempo.TiempoPruebas;
import com.techprotech.agenda.modulos.admin.aplicacion.ServicioAdminCitas;
import com.techprotech.agenda.modulos.citas.dominio.eventos.*;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.disponibilidad.aplicacion.*;
import com.techprotech.agenda.modulos.disponibilidad.api.dto.ConfiguracionReservasResponse;
import com.techprotech.agenda.modulos.prestadores.infraestructura.entidad.PrestadorServicioEntidad;
import com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio.PrestadorServicioRepositorio;
import com.techprotech.agenda.modulos.recepcion.aplicacion.ServicioRecepcion;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventosConfirmacionReprogramacionTest {
    @Mock CitaRepositorio citas;
    @Mock HistorialEstadoCitaRepositorio historial;
    @Mock ApplicationEventPublisher bus;
    @Mock ClienteRepositorio clientes;
    @Mock UsuarioRepositorio usuarios;
    @Mock SucursalRepositorio sucursales;
    @Mock ServicioRepositorio servicios;
    @Mock PrestadorServicioRepositorio prestadores;
    @Mock ServicioConsultaDisponibilidad disponibilidad;
    @Mock ServicioConfiguracionReservas reservas;
    @InjectMocks ServicioAdminCitas admin;
    @InjectMocks ServicioRecepcion recepcion;
    final Clock reloj = Clock.fixed(Instant.parse("2026-10-16T18:00:00Z"), ZoneOffset.UTC);
    ServicioCitasCliente cliente;
    CitaEntidad cita;

    @BeforeEach void preparar() {
        var eventos = new PublicadorEventosCita(bus, reloj);
        cliente = new ServicioCitasCliente(citas, historial, sucursales, servicios, prestadores, disponibilidad,
                clientes, usuarios, eventos, reservas, reloj, TiempoPruebas.zona(ZoneId.of("America/Mexico_City")));
        ReflectionTestUtils.setField(admin, "eventos", eventos);
        ReflectionTestUtils.setField(recepcion, "eventos", eventos);
        ReflectionTestUtils.setField(recepcion, "reloj", reloj);
        ReflectionTestUtils.setField(recepcion, "servicioCitasCliente", cliente);
        var saldos = new com.techprotech.agenda.modulos.caja.aplicacion.ServicioSaldoCitas(citas,
                mock(com.techprotech.agenda.modulos.caja.infraestructura.repositorio.PagoCitaRepositorio.class),
                mock(com.techprotech.agenda.modulos.caja.infraestructura.repositorio.VentaDetalleRepositorio.class));
        ReflectionTestUtils.setField(recepcion, "cobros", new com.techprotech.agenda.modulos.caja.aplicacion.ServicioConsultaCobroCitas(saldos,null,null));
        ReflectionTestUtils.setField(recepcion, "cierres", new ServicioCierreCita(citas,historial,eventos,reloj,TiempoPruebas.zonaUtc()));
        ReflectionTestUtils.setField(recepcion, "cancelaciones", new ServicioCancelacionCita(citas,saldos,historial,eventos,reloj));
        cita = CierreCitaUnificadoTest.cita(); cita.setEstado("PENDIENTE"); cita.setMoneda("MXN");
        cita.setInicio(LocalDateTime.parse("2026-11-20T10:00")); cita.setFin(cita.getInicio().plusHours(1));
        lenient().when(citas.findByIdAndEmpresaId(1L,7L)).thenReturn(Optional.of(cita));
        lenient().when(citas.bloquearPorIdYEmpresa(1L,7L)).thenReturn(Optional.of(cita));
        lenient().when(citas.findByIdAndEmpresaIdAndClienteId(1L,7L,20L)).thenReturn(Optional.of(cita));
        lenient().when(citas.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @ParameterizedTest @CsvSource({"ADMIN,ADMIN,11,false", "RECEPCION,RECEPCION,11,false",
            "CHECK_IN,RECEPCION,11,false", "CLIENTE,CLIENTE,20,true"})
    void confirmacionEmiteUnHechoConElActorRealYSinCambiarCanales(String ruta, OrigenEventoCita origen, long actor, boolean whatsapp) {
        if (ruta.equals("CLIENTE")) {
            when(sucursales.findByIdAndEmpresaIdAndActivaTrue(3L,7L)).thenReturn(Optional.of(new SucursalEntidad()));
            when(reservas.obtener(7L)).thenReturn(new ConfiguracionReservasResponse(0,30,2,15,2));
            cliente.confirmar(7L,20L,1L);
        } else if (ruta.equals("ADMIN")) admin.cambiarEstadoCita(CierreCitaUnificadoTest.actor(),1L,"CONFIRMADA");
        else if (ruta.equals("CHECK_IN")) recepcion.checkIn(CierreCitaUnificadoTest.actor(),1L);
        else recepcion.confirmar(CierreCitaUnificadoTest.actor(),1L);
        var captor = ArgumentCaptor.forClass(CitaConfirmada.class);
        verify(bus).publishEvent(captor.capture()); verifyNoMoreInteractions(bus);
        assertThat(captor.getValue().datos().origen()).isEqualTo(origen);
        assertThat(captor.getValue().datos().actorId()).isEqualTo(actor);
        assertThat(captor.getValue().datos().momento()).isEqualTo(reloj.instant());
        assertThat(captor.getValue().avisarWhatsapp()).isEqualTo(whatsapp);
        assertThat(captor.getValue().avisarNegocio()).isFalse();
    }

    @Test void cancelacionClienteNoUsaLaPoliticaDelNegocioNiDuplicaEvento() {
        cliente.cancelar(7L,20L,1L);
        verify(bus).publishEvent(argThat((Object e) -> e instanceof CitaCancelada c
                && c.datos().origen() == OrigenEventoCita.CLIENTE && c.datos().actorId().equals(20L)));
        assertThatThrownBy(() -> cliente.cancelar(7L,20L,1L))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verifyNoMoreInteractions(bus);
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void reprogramacionConservaPrecioYPublicaSnapshotAnteriorYOrigen(boolean desdeRecepcion) {
        when(prestadores.bloquearPorEmpresa(7L,11L)).thenReturn(Optional.of(new PrestadorServicioEntidad()));
        var nuevo = OffsetDateTime.parse("2026-11-22T13:00:00-06:00");
        when(disponibilidad.obtenerFranjasDisponibles(7L,3L,5L,11L,nuevo.toLocalDate()))
                .thenReturn(List.of(new FranjaDisponibleResponse(nuevo.toString(),nuevo.plusHours(1).toString(),11L,5L,3L)));
        var anterior = cita.getInicio();
        if (desdeRecepcion) recepcion.reprogramar(7L,11L,List.of(3L),1L,
                new com.techprotech.agenda.modulos.recepcion.api.dto.ReagendarRecepcionRequest(nuevo));
        else cliente.reprogramar(7L,20L,1L,nuevo);
        var captor = ArgumentCaptor.forClass(CitaReprogramada.class); verify(bus).publishEvent(captor.capture());
        verifyNoMoreInteractions(bus);
        assertThat(captor.getValue().inicioAnterior()).isEqualTo(anterior);
        assertThat(captor.getValue().datos().inicio()).isEqualTo(nuevo.toLocalDateTime());
        assertThat(captor.getValue().datos().origen()).isEqualTo(desdeRecepcion ? OrigenEventoCita.RECEPCION : OrigenEventoCita.CLIENTE);
        assertThat(captor.getValue().datos().actorId()).isEqualTo(desdeRecepcion ? 11L : 20L);
        assertThat(cita.getPrecio()).isEqualByComparingTo(new BigDecimal("500"));
        verify(historial).save(argThat(h -> h.getCambiadoPorUsuarioId().equals(desdeRecepcion ? 11L : 20L)
                && h.getMotivo().contains(desdeRecepcion ? "recepción" : "cliente")));
    }
}
