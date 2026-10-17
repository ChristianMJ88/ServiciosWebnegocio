package com.techprotech.agenda.modulos.citas.aplicacion;

import com.techprotech.agenda.compartido.correo.ServicioOutboxCorreoCitas;
import com.techprotech.agenda.compartido.whatsapp.ServicioOutboxWhatsappCitas;
import com.techprotech.agenda.compartido.tiempo.ZonaNegocio;
import com.techprotech.agenda.modulos.admin.aplicacion.ServicioAdminCitas;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.caja.aplicacion.*;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.clientes.infraestructura.entidad.ClienteEntidad;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.prestadores.aplicacion.ServicioAgendaStaff;
import com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio.PrestadorServicioRepositorio;
import com.techprotech.agenda.modulos.recepcion.aplicacion.ServicioRecepcion;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CierreCitaUnificadoTest {
    @Mock org.springframework.context.ApplicationEventPublisher observador;
    @Mock CitaRepositorio citas;
    @Mock HistorialEstadoCitaRepositorio historial;
    @Mock ServicioOutboxCorreoCitas correo;
    @Mock ServicioOutboxWhatsappCitas whatsapp;
    @Mock ClienteRepositorio clientes;
    @Mock SucursalRepositorio sucursales;
    @Mock PrestadorServicioRepositorio prestadores;
    @Mock ServicioRepositorio servicios;
    @Mock UsuarioRepositorio usuarios;
    @Mock PagoCitaRepositorio pagos;
    @Mock VentaDetalleRepositorio ventas;
    @Mock CajaSesionRepositorio sesiones;
    @Mock ZonaNegocio zonas;
    @InjectMocks ServicioRecepcion recepcion;
    @InjectMocks ServicioAgendaStaff staff;
    @InjectMocks ServicioAdminCitas admin;
    ServicioCierreCita cierre;
    ServicioConsultaCobroCitas cobros;
    Clock reloj=Clock.fixed(Instant.parse("2026-10-15T16:00:00Z"),ZoneOffset.UTC);

    @BeforeEach void preparar() {
        cierre=new ServicioCierreCita(citas, historial, com.techprotech.agenda.modulos.citas.aplicacion.EventosCitaPruebas.publicador(correo, whatsapp, clientes, reloj), reloj, zonas);
        var notificaciones = EventosCitaPruebas.publicador(correo, whatsapp, clientes, reloj);
        var eventos = new PublicadorEventosCita(evento -> { observador.publishEvent(evento); notificaciones.publicar((com.techprotech.agenda.modulos.citas.dominio.eventos.EventoCita) evento); }, reloj);
        ReflectionTestUtils.setField(cierre, "eventos", eventos);
        org.springframework.test.util.ReflectionTestUtils.setField(recepcion, "eventos", eventos);
        org.springframework.test.util.ReflectionTestUtils.setField(admin, "eventos", eventos);
        cobros=new ServicioConsultaCobroCitas(new ServicioSaldoCitas(citas,pagos,ventas),sesiones,null);
        for(Object servicio:List.of(recepcion,staff,admin)) {
            ReflectionTestUtils.setField(servicio,"cierres",cierre);
            ReflectionTestUtils.setField(servicio,"cobros",cobros);
            ReflectionTestUtils.setField(servicio,"reloj",reloj);
        }
        var cancelaciones = new ServicioCancelacionCita(citas, new ServicioSaldoCitas(citas,pagos,ventas), historial, eventos, reloj);
        ReflectionTestUtils.setField(recepcion,"cancelaciones",cancelaciones);
        ReflectionTestUtils.setField(admin,"cancelaciones",cancelaciones);
        lenient().when(zonas.de(7L,3L)).thenReturn(ZoneId.of("America/Mexico_City"));
    }

    @Test void confirmarDesdeStaffConservaElComportamientoSinNotificacionesNuevasEnFaseUno() {
        var cita=cita(); cita.setEstado("PENDIENTE");
        when(citas.findByIdAndEmpresaIdAndPrestadorId(1L,7L,11L)).thenReturn(Optional.of(cita));
        staff.cambiarEstado(actor(),1L,"CONFIRMADA");
        assertThat(cita.getEstado()).isEqualTo("CONFIRMADA");
        verify(historial).save(argThat(h -> h.getCambiadoPorUsuarioId().equals(11L) && h.getEstadoNuevo().equals("CONFIRMADA")));
        verifyNoInteractions(observador,correo,whatsapp);
    }

    @ParameterizedTest @EnumSource(OrigenCierreCita.class)
    void finalizarDesdeLasTresRutasDevuelveMismoSaldoHistorialYNotificacionesSinExigirCheckin(OrigenCierreCita origen) {
        var cita=cita();when(citas.bloquearPorIdYEmpresa(1L,7L)).thenReturn(Optional.of(cita));
        var cliente=new ClienteEntidad();cliente.setAceptaWhatsapp(true);cliente.setTelefono("5555555555");
        when(clientes.findByClienteIdAndEmpresaId(20L,7L)).thenReturn(Optional.of(cliente));
        switch(origen) {
            case RECEPCION -> { var r=recepcion.finalizar(actor(),1L);assertThat(r.pendiente()).isEqualByComparingTo("500");assertThat(r.estadoCobro()).isEqualTo(EstadoCobroCita.PENDIENTE);assertThat(r.puedeCobrar()).isFalse(); }
            case STAFF -> comprobarRespuesta(staff.cambiarEstado(actor(),1L,"FINALIZADA"));
            case ADMIN -> comprobarRespuesta(admin.cambiarEstadoCita(actor(),1L,"FINALIZADA"));
        }
        assertThat(cita.getEstado()).isEqualTo("FINALIZADA");assertThat(cita.getCheckInEn()).isNull();
        var captor=ArgumentCaptor.forClass(HistorialEstadoCitaEntidad.class);verify(historial).save(captor.capture());
        assertThat(captor.getValue().getCambiadoPorUsuarioId()).isEqualTo(11L);
        assertThat(captor.getValue().getMotivo()).contains(origen.name(),"finalizada sin check-in");
        verify(correo).programarGraciasVisita(7L,1L,cita.getInicio());
        verify(whatsapp).programarGraciasVisita(7L,1L,cliente.getTelefono(),cita.getInicio());
        cierre.cerrar(actor(),1L,"FINALIZADA",origen);
        verify(historial,times(1)).save(any());verifyNoMoreInteractions(correo,whatsapp);
        verify(observador).publishEvent(argThat((Object e) -> e instanceof com.techprotech.agenda.modulos.citas.dominio.eventos.CitaFinalizada f
                && f.datos().origen().name().equals(origen.name()) && f.datos().actorId().equals(11L)));
        verifyNoMoreInteractions(observador);
    }

    @ParameterizedTest @EnumSource(OrigenCierreCita.class)
    void noAsistioMantieneAnticipoSinDeudaNiAccionDeCobroYNotificaUnaSolaVez(OrigenCierreCita origen) {
        var cita=cita();when(citas.bloquearPorIdYEmpresa(1L,7L)).thenReturn(Optional.of(cita));
        when(ventas.sumarServiciosPagadosPorCitas(7L,List.of(1L))).thenReturn(Collections.singletonList(new Object[]{1L,new BigDecimal("80")}));
        cierre.cerrar(actor(),1L,"NO_ASISTIO",origen);
        var r=cobros.respuesta(actor(),cita);
        assertThat(r.pagado()).isEqualByComparingTo("80");assertThat(r.pendiente()).isZero();
        assertThat(r.estadoCobro()).isEqualTo(EstadoCobroCita.NO_COBRABLE);assertThat(r.puedeCobrar()).isFalse();
        cierre.cerrar(actor(),1L,"NO_ASISTIO",origen);
        verify(correo).programarNoAsistio(7L,1L,cita.getInicio());verifyNoMoreInteractions(correo);verifyNoInteractions(whatsapp);
        verify(historial,times(1)).save(any());
        verify(observador).publishEvent(isA(com.techprotech.agenda.modulos.citas.dominio.eventos.CitaNoAsistio.class));
        verifyNoMoreInteractions(observador);
    }

    @ParameterizedTest @EnumSource(OrigenCierreCita.class)
    void antesDelInicioLocalSeRechazaEnLasTresRutasAunqueUtcYaSupereLaHoraNominal(OrigenCierreCita origen) {
        var cita=cita();cita.setInicio(cita.getInicio().plusMinutes(1));when(citas.bloquearPorIdYEmpresa(1L,7L)).thenReturn(Optional.of(cita));
        assertThatThrownBy(() -> cerrarRuta(origen)).isInstanceOfSatisfying(ResponseStatusException.class,e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
        assertThat(cita.getEstado()).isEqualTo("CONFIRMADA");verify(citas,never()).save(any());verifyNoInteractions(historial,correo,whatsapp);
    }

    @ParameterizedTest @CsvSource({"STAFF,12,3,404", "RECEPCION,11,9,404", "ADMIN,11,9,404"})
    void propiedadYAlcanceSeValidanAntesDeCualquierCambio(OrigenCierreCita origen,long prestador,long sucursal,int status) {
        var cita=cita();cita.setPrestadorId(prestador);cita.setSucursalId(sucursal);
        when(citas.bloquearPorIdYEmpresa(1L,7L)).thenReturn(Optional.of(cita));
        assertThatThrownBy(() -> cierre.cerrar(actor(),1L,"FINALIZADA",origen)).isInstanceOfSatisfying(ResponseStatusException.class,e -> assertThat(e.getStatusCode().value()).isEqualTo(status));
        verifyNoInteractions(historial,correo,whatsapp);verify(citas,never()).save(any());
    }

    @ParameterizedTest @CsvSource({"500,0,PAGADA", "100,400,PARCIAL", "0,500,PENDIENTE"})
    void finalizarConPagoOnlineInformaLoAcreditadoSinCrearOtroPago(String online,String pendiente,EstadoCobroCita estado) {
        var cita=cita();when(citas.bloquearPorIdYEmpresa(1L,7L)).thenReturn(Optional.of(cita));
        when(ventas.sumarServiciosPagadosPorCitas(7L,List.of(1L))).thenReturn(Collections.singletonList(new Object[]{1L,new BigDecimal(online)}));
        var r=recepcion.finalizar(actor(),1L);
        assertThat(r.pagado()).isEqualByComparingTo(online);assertThat(r.pendiente()).isEqualByComparingTo(pendiente);assertThat(r.estadoCobro()).isEqualTo(estado);
        verify(pagos,never()).save(any());verify(ventas,never()).save(any());
    }

    @Test void otraEmpresaNoSeBuscaPorIdGlobalNiDisparaEfectos() {
        assertThatThrownBy(() -> cierre.cerrar(actor(),1L,"FINALIZADA",OrigenCierreCita.ADMIN)).isInstanceOfSatisfying(ResponseStatusException.class,e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
        verify(citas).bloquearPorIdYEmpresa(1L,7L);verifyNoMoreInteractions(citas);verifyNoInteractions(historial,correo,whatsapp);
    }

    @ParameterizedTest @CsvSource({"PENDIENTE_PAGO,409", "CANCELADA,400", "NO_ASISTIO,400", "LIBERADA,400"})
    void estadosNoAdmitidosNoSeReabren(String estado,int status) {
        var cita=cita();cita.setEstado(estado);when(citas.bloquearPorIdYEmpresa(1L,7L)).thenReturn(Optional.of(cita));
        assertThatThrownBy(() -> cierre.cerrar(actor(),1L,"FINALIZADA",OrigenCierreCita.ADMIN)).isInstanceOfSatisfying(ResponseStatusException.class,e -> assertThat(e.getStatusCode().value()).isEqualTo(status));
        verifyNoInteractions(historial,correo,whatsapp);
    }

    @Test void elegibilidadCambiaAlInicioYElContratoAdminLoExponeSinUnaReglaDeRelojEnFrontend() throws Exception {
        var cita=cita();cita.setInicio(cita.getInicio().plusSeconds(1));
        assertThat(cierre.puedeCerrar(cita)).isFalse();
        var despues=new ServicioCierreCita(citas, historial, com.techprotech.agenda.modulos.citas.aplicacion.EventosCitaPruebas.publicador(correo, whatsapp, clientes, Clock.offset(reloj,Duration.ofSeconds(1))), Clock.offset(reloj,Duration.ofSeconds(1)), zonas);
        assertThat(despues.puedeCerrar(cita)).isTrue();
        when(citas.findByEmpresaIdAndInicioBetweenOrderByInicioAsc(7L,LocalDate.of(2026,10,15).atStartOfDay(),LocalDate.of(2026,10,16).atStartOfDay())).thenReturn(List.of(cita));
        var r=admin.listar(actor(),LocalDate.of(2026,10,15),LocalDate.of(2026,10,15)).getFirst();
        var json=new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules().valueToTree(r);
        assertThat(json.get("id").asLong()).isEqualTo(1L);
        assertThat(json.get("puedeCerrar").asBoolean()).isFalse();
        assertThat(json.has("cita")).isFalse();
    }

    @Test void elegibilidadPorLoteResuelveLaZonaUnaVezPorSucursal() {
        var citasDelDia=new java.util.ArrayList<CitaEntidad>();
        for(long id=1;id<=100;id++) {var c=cita();c.setId(id);citasDelDia.add(c);}
        assertThat(cierre.elegibilidad(citasDelDia)).hasSize(100).allSatisfy((id,puede) -> assertThat(puede).isTrue());
        verify(zonas,times(1)).de(7L,3L);
    }

    @ParameterizedTest @CsvSource({"FINALIZADA", "NO_ASISTIO", "CANCELADA"})
    void administracionNoCancelaAtencionesYaCerradas(String estado) {
        var cita=cita();cita.setEstado(estado);when(citas.bloquearPorIdYEmpresa(1L,7L)).thenReturn(Optional.of(cita));
        assertThatThrownBy(() -> admin.cambiarEstadoCita(actor(),1L,"CANCELADA")).isInstanceOfSatisfying(ResponseStatusException.class,e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
        assertThat(cita.getEstado()).isEqualTo(estado);verifyNoInteractions(historial,correo,whatsapp,pagos,ventas);
    }

    @Test void administracionNoCancelaUnaCitaConAnticipoSinReembolsoConciliado() {
        var cita=cita();when(citas.bloquearPorIdYEmpresa(1L,7L)).thenReturn(Optional.of(cita));
        when(ventas.sumarServiciosPagadosPorCitas(7L,List.of(1L))).thenReturn(Collections.singletonList(new Object[]{1L,new BigDecimal("80")}));
        assertThatThrownBy(() -> admin.cambiarEstadoCita(actor(),1L,"CANCELADA")).isInstanceOfSatisfying(ResponseStatusException.class,e -> {assertThat(e.getStatusCode().value()).isEqualTo(409);assertThat(e.getReason()).contains("reprogramarla");});
        assertThat(cita.getEstado()).isEqualTo("CONFIRMADA");verifyNoInteractions(historial,correo,whatsapp);verify(citas,never()).save(any());
    }

    @ParameterizedTest @CsvSource({"ADMIN,CAJA", "ADMIN,ONLINE", "RECEPCION,CAJA", "RECEPCION,ONLINE"})
    void ambasRutasRechazanPagosSinHistorialNiOutbox(OrigenCierreCita origen,String fuente) {
        var cita=cita();when(citas.bloquearPorIdYEmpresa(1L,7L)).thenReturn(Optional.of(cita));
        if ("CAJA".equals(fuente)) when(pagos.sumarPagadoPorCitas(7L,List.of(1L))).thenReturn(Collections.singletonList(new Object[]{1L,new BigDecimal("80")}));
        else when(ventas.sumarServiciosPagadosPorCitas(7L,List.of(1L))).thenReturn(Collections.singletonList(new Object[]{1L,new BigDecimal("80")}));
        assertThatThrownBy(() -> cancelarRuta(origen)).isInstanceOfSatisfying(ResponseStatusException.class,
            e -> {assertThat(e.getStatusCode().value()).isEqualTo(409);assertThat(e.getReason()).contains("reprogramarla");});
        assertThat(cita.getEstado()).isEqualTo("CONFIRMADA");assertThat(cita.getCanceladaEn()).isNull();
        verify(citas,never()).save(any());verifyNoInteractions(historial,correo,whatsapp);
        var orden=inOrder(citas,pagos);orden.verify(citas).bloquearPorIdYEmpresa(1L,7L);orden.verify(pagos).sumarPagadoPorCitas(7L,List.of(1L));
    }

    @ParameterizedTest @EnumSource(value=OrigenCierreCita.class,names={"ADMIN","RECEPCION"})
    void ambasRutasRechazanCheckinAunqueNoHayaPagos(OrigenCierreCita origen) {
        var cita=cita();cita.setCheckInEn(LocalDateTime.now(reloj));when(citas.bloquearPorIdYEmpresa(1L,7L)).thenReturn(Optional.of(cita));
        assertThatThrownBy(() -> cancelarRuta(origen)).isInstanceOfSatisfying(ResponseStatusException.class,
            e -> {assertThat(e.getStatusCode().value()).isEqualTo(409);assertThat(e.getReason()).contains("check-in");});
        assertThat(cita.getEstado()).isEqualTo("CONFIRMADA");verify(citas,never()).save(any());
        verifyNoInteractions(historial,correo,whatsapp,pagos,ventas);
    }

    @ParameterizedTest @CsvSource({"ADMIN,PENDIENTE", "ADMIN,CONFIRMADA", "RECEPCION,PENDIENTE", "RECEPCION,CONFIRMADA"})
    void sinPagosNiLlegadaAmbasRutasCancelanConAuditoriaYNotificaciones(OrigenCierreCita origen,String estado) {
        var cita=cita();cita.setEstado(estado);when(citas.bloquearPorIdYEmpresa(1L,7L)).thenReturn(Optional.of(cita));
        var cliente=new ClienteEntidad();cliente.setAceptaWhatsapp(true);cliente.setTelefono("5555555555");
        when(clientes.findByClienteIdAndEmpresaId(20L,7L)).thenReturn(Optional.of(cliente));
        cancelarRuta(origen);
        assertThat(cita.getEstado()).isEqualTo("CANCELADA");assertThat(cita.getCanceladaEn()).isNotNull();
        verify(historial).save(argThat(h -> estado.equals(h.getEstadoAnterior()) && "CANCELADA".equals(h.getEstadoNuevo()) && h.getCambiadoPorUsuarioId().equals(11L)));
        verify(correo).programarCanceladaNegocio(7L,1L,cita.getInicio());
        verify(whatsapp).programarCancelacionNegocio(7L,1L,cliente.getTelefono(),cita.getInicio());
        verify(clientes,never()).findById(any());
        verify(observador).publishEvent(argThat((Object e) -> e instanceof com.techprotech.agenda.modulos.citas.dominio.eventos.CitaCancelada c
                && c.datos().origen().name().equals(origen.name()) && c.datos().empresaId().equals(7L)));
        verifyNoMoreInteractions(observador);
    }

    @ParameterizedTest @CsvSource({"ADMIN,false", "ADMIN,true", "RECEPCION,false", "RECEPCION,true"})
    void cancelarNoRevelaNiModificaCitasFueraDeEmpresaOSucursal(OrigenCierreCita origen,boolean fueraSucursal) {
        if(fueraSucursal) {var cita=cita();cita.setSucursalId(9L);when(citas.bloquearPorIdYEmpresa(1L,7L)).thenReturn(Optional.of(cita));}
        assertThatThrownBy(() -> cancelarRuta(origen)).isInstanceOfSatisfying(ResponseStatusException.class,e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
        verify(citas).bloquearPorIdYEmpresa(1L,7L);verifyNoMoreInteractions(citas);verifyNoInteractions(historial,correo,whatsapp,pagos,ventas);
    }

    @Test void agendasCompartenElegibilidadDeCancelacionSinConsultasDePagoPorCita() {
        var libre=cita();var anticipo=cita();anticipo.setId(2L);var llegada=cita();llegada.setId(3L);llegada.setCheckInEn(LocalDateTime.now(reloj));
        var cerrada=cita();cerrada.setId(4L);cerrada.setEstado("FINALIZADA");
        var lista=List.of(libre,anticipo,llegada,cerrada);
        when(citas.findByEmpresaIdAndInicioBetweenOrderByInicioAsc(eq(7L),any(),any())).thenReturn(lista);
        when(ventas.sumarServiciosPagadosPorCitas(7L,List.of(1L,2L,3L,4L))).thenReturn(Collections.singletonList(new Object[]{2L,new BigDecimal("80")}));
        var dia=LocalDate.of(2026,10,15);
        var a=admin.listar(actor(),dia,dia);
        assertThat(a).extracting(r -> r.puedeCancelar()).containsExactly(true,false,false,false);
        verify(pagos).sumarPagadoPorCitas(7L,List.of(1L,2L,3L,4L));
        clearInvocations(pagos,ventas);
        var r=recepcion.agenda(actor(),dia,3L);
        assertThat(r).extracting(c -> c.puedeCancelar()).containsExactly(true,false,false,false);
        verify(pagos).sumarPagadoPorCitas(7L,List.of(1L,2L,3L,4L));
        verify(ventas).sumarServiciosPagadosPorCitas(7L,List.of(1L,2L,3L,4L));verifyNoMoreInteractions(pagos,ventas);
    }

    @Test void checkinTambienBloqueaLaCitaParaSerializarseConLaCancelacion() {
        var cita=cita();when(citas.bloquearPorIdYEmpresa(1L,7L)).thenReturn(Optional.of(cita));
        when(citas.save(any())).thenAnswer(i -> i.getArgument(0));
        var r=recepcion.checkIn(actor(),1L);
        assertThat(r.checkInEn()).isNotNull();assertThat(r.puedeCancelar()).isFalse();
        verify(citas).bloquearPorIdYEmpresa(1L,7L);verify(citas,never()).findByIdAndEmpresaId(any(),any());
    }

    void cancelarRuta(OrigenCierreCita origen) {
        if(origen==OrigenCierreCita.ADMIN) admin.cambiarEstadoCita(actor(),1L,"CANCELADA");
        else recepcion.cancelar(actor(),1L);
    }

    void cerrarRuta(OrigenCierreCita origen) {
        switch(origen) {
            case RECEPCION -> recepcion.finalizar(actor(),1L);
            case STAFF -> staff.cambiarEstado(actor(),1L,"FINALIZADA");
            case ADMIN -> admin.cambiarEstadoCita(actor(),1L,"FINALIZADA");
        }
    }
    static void comprobarRespuesta(com.techprotech.agenda.modulos.citas.api.dto.ResultadoCierreCitaResponse r) {
        assertThat(r.pendiente()).isEqualByComparingTo("500");assertThat(r.pagado()).isZero();assertThat(r.estadoCobro()).isEqualTo(EstadoCobroCita.PENDIENTE);assertThat(r.puedeCobrar()).isFalse();
    }
    static UsuarioAutenticado actor() { return new UsuarioAutenticado(null,11L,7L,"EMPRESA",List.of(),List.of(),List.of(3L)); }
    static CitaEntidad cita() {
        var c=new CitaEntidad();c.setId(1L);c.setEmpresaId(7L);c.setSucursalId(3L);c.setPrestadorId(11L);c.setClienteId(20L);c.setServicioId(5L);
        c.setEstado("CONFIRMADA");c.setPrecio(new BigDecimal("500"));c.setInicio(LocalDateTime.parse("2026-10-15T10:00"));c.setFin(c.getInicio().plusHours(1));return c;
    }
}
