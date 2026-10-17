package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.compartido.correo.ServicioOutboxCorreoCitas;
import com.techprotech.agenda.compartido.whatsapp.ServicioOutboxWhatsappCitas;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.disponibilidad.aplicacion.ServicioConfiguracionReservas;
import com.techprotech.agenda.modulos.disponibilidad.api.dto.ConfiguracionReservasResponse;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenDetalleEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenDetalleRepositorio;
import com.techprotech.agenda.modulos.prestadores.infraestructura.entidad.PrestadorServicioEntidad;
import com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio.PrestadorServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ServicioConfirmacionCitasCheckoutTest {
    final CheckoutOrdenDetalleRepositorio detalles=mock(CheckoutOrdenDetalleRepositorio.class);
    final CitaRepositorio citas=mock(CitaRepositorio.class);
    final HistorialEstadoCitaRepositorio historial=mock(HistorialEstadoCitaRepositorio.class);
    final ServicioOutboxCorreoCitas correo=mock(ServicioOutboxCorreoCitas.class);
    final ServicioOutboxWhatsappCitas whatsapp=mock(ServicioOutboxWhatsappCitas.class);
    final EntityManager em=mock(EntityManager.class);
    final PrestadorServicioRepositorio prestadores=mock(PrestadorServicioRepositorio.class);
    final SucursalRepositorio sucursales=mock(SucursalRepositorio.class);
    final ServicioConfiguracionReservas reservas=mock(ServicioConfiguracionReservas.class);
    final Clock reloj=Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"),ZoneOffset.UTC);
    final org.springframework.context.ApplicationEventPublisher bus=mock(org.springframework.context.ApplicationEventPublisher.class);
    final ServicioConfirmacionCitasCheckout servicio=new ServicioConfirmacionCitasCheckout(detalles, citas, historial, eventos(), em, prestadores, sucursales, reservas, reloj);
    final CitaEntidad a=miembro(18L),b=miembro(19L);
    private com.techprotech.agenda.modulos.citas.aplicacion.PublicadorEventosCita eventos() {
        var notificaciones=com.techprotech.agenda.modulos.citas.aplicacion.EventosCitaPruebas.publicador(correo,whatsapp,mock(ClienteRepositorio.class),reloj);
        return new com.techprotech.agenda.modulos.citas.aplicacion.PublicadorEventosCita(evento -> {
            bus.publishEvent(evento); notificaciones.publicar((com.techprotech.agenda.modulos.citas.dominio.eventos.EventoCita)evento);
        },reloj);
    }
    @BeforeEach void preparar() {
        var detalle=new CheckoutOrdenDetalleEntidad();detalle.setTipoConcepto("SERVICIO");detalle.setCitaId(18L);
        when(detalles.findByEmpresaIdAndCheckoutOrdenIdOrderByIdAsc(7L,30L)).thenReturn(List.of(detalle));
        var referencia=mock(CitaRepositorio.ReferenciaReserva.class);
        when(referencia.getEmpresaId()).thenReturn(7L);when(referencia.getReservaGrupoId()).thenReturn("grupo");
        when(citas.buscarReferenciaReserva(18L)).thenReturn(Optional.of(referencia));
        when(citas.buscarPrestadoresReserva(7L,18L,"grupo")).thenReturn(List.of(8L));
        when(citas.findByEmpresaIdAndReservaGrupoIdOrderByIdAsc(7L,"grupo")).thenReturn(List.of(a,b));
        when(prestadores.bloquearPorEmpresa(7L,8L)).thenReturn(Optional.of(new PrestadorServicioEntidad()));
        var sucursal=new SucursalEntidad();sucursal.setZonaHoraria("America/Mexico_City");
        when(sucursales.findByIdAndEmpresaIdAndActivaTrue(3L,7L)).thenReturn(Optional.of(sucursal));
        when(reservas.obtener(7L)).thenReturn(new ConfiguracionReservasResponse(0,30,2,15,2));
    }
    @Test void confirmaElGrupoIncluidoMiembroSinCobroUnaSolaVez() {
        b.setPoliticaCobro("SIN_COBRO");
        assertThat(servicio.confirmar(7L,30L)).isTrue(); assertThat(servicio.confirmar(7L,30L)).isTrue();
        assertThat(List.of(a.getEstado(),b.getEstado())).containsOnly("CONFIRMADA");
        verify(historial,times(2)).save(any());verify(correo,times(2)).programarConfirmada(eq(7L),anyLong(),any());
        verify(bus,times(2)).publishEvent(argThat((Object e) -> e instanceof com.techprotech.agenda.modulos.citas.dominio.eventos.CitaConfirmada c
                && c.datos().origen()==com.techprotech.agenda.modulos.citas.dominio.eventos.OrigenEventoCita.WEBHOOK && c.datos().actorId()==null
                && c.avisarNegocio() && c.avisarWhatsapp()));
        verifyNoMoreInteractions(bus);
    }
    @Test void resolucionAdministrativaConservaActorRealYLosMismosCanalesDelPago() {
        servicio.confirmar(7L,30L,com.techprotech.agenda.modulos.citas.dominio.eventos.OrigenEventoCita.ADMIN,9L);
        verify(bus,times(2)).publishEvent(argThat((Object e) -> e instanceof com.techprotech.agenda.modulos.citas.dominio.eventos.CitaConfirmada c
                && c.datos().origen()==com.techprotech.agenda.modulos.citas.dominio.eventos.OrigenEventoCita.ADMIN
                && c.datos().actorId().equals(9L) && c.avisarNegocio() && c.avisarWhatsapp()));
        verifyNoMoreInteractions(bus);
        verify(correo,times(2)).programarConfirmada(eq(7L),anyLong(),any());
        verify(correo,times(2)).programarNuevaCitaNegocio(eq(7L),anyLong(),any());
    }
    @Test void pagoTardioRecuperaHorarioLibreYConfirmaTodoElGrupo() {
        a.setEstado("LIBERADA_SIN_PAGO");b.setEstado("LIBERADA_SIN_PAGO");
        assertThat(servicio.confirmar(7L,30L)).isTrue();
        assertThat(List.of(a.getEstado(),b.getEstado())).containsOnly("CONFIRMADA");
        verify(prestadores).bloquearPorEmpresa(7L,8L);
        verify(citas).buscarConflictosBloqueando(eq(8L),eq(a.getInicio()),eq(a.getFin()),any());
        verify(correo).programarConfirmada(7L,18L,a.getInicio());
    }
    @Test void pagoTardioConConflictoDevuelveIncidenciaSinAvisarConfirmacion() {
        a.setEstado("LIBERADA_SIN_PAGO");b.setEstado("LIBERADA_SIN_PAGO");
        var ocupada=miembro(99L);
        when(citas.buscarConflictosBloqueando(eq(8L),eq(a.getInicio()),eq(a.getFin()),any())).thenReturn(List.of(ocupada));
        assertThat(servicio.confirmar(7L,30L)).isFalse();
        assertThat(a.getEstado()).isEqualTo("LIBERADA_SIN_PAGO");verifyNoInteractions(correo,whatsapp,historial);
    }
    @Test void refrescaDespuesDelBloqueoYNoLiberaUnaCitaYaPagada() {
        when(citas.buscarIdsApartadosVencidos(any(),any())).thenReturn(List.of(18L));
        doAnswer(i -> { a.setEstado("CONFIRMADA");b.setEstado("CONFIRMADA");return null; })
                .when(em).refresh(a,LockModeType.PESSIMISTIC_WRITE);
        servicio.liberarCitasVencidas();
        assertThat(a.getEstado()).isEqualTo("CONFIRMADA");verifyNoInteractions(historial,correo,whatsapp);
        var orden=inOrder(citas,prestadores,em);
        orden.verify(citas).buscarIdsApartadosVencidos(any(),any());
        orden.verify(citas).buscarReferenciaReserva(18L);
        orden.verify(citas).buscarPrestadoresReserva(7L,18L,"grupo");
        orden.verify(prestadores).bloquearPorEmpresa(7L,8L);
        orden.verify(citas).findByEmpresaIdAndReservaGrupoIdOrderByIdAsc(7L,"grupo");
        orden.verify(em).refresh(a,LockModeType.PESSIMISTIC_WRITE);
        verify(citas,never()).findById(any());
    }
    @Test void topeSinFechaEsConfigurableYSeLiberaSinAvisoDeCancelacion() {
        var limite=servicio.prorrogar(7L,30L,null);
        assertThat(limite.toInstant()).isEqualTo(reloj.instant().plus(Duration.ofHours(2)));
        assertThat(a.getPagoExpiraEn()).isEqualTo(LocalDateTime.now(reloj).plusHours(2));
        servicio.liberar(7L,30L);servicio.liberar(7L,30L);
        verify(correo).programarApartadoVencido(7L,18L,a.getInicio());
        verify(correo,never()).programarCanceladaCliente(any(),any(),any());verifyNoInteractions(whatsapp);
        verify(historial,times(2)).save(any());
    }
    @Test void vencimientoInformadoSeRespetaAntesDeLiberar() {
        when(citas.buscarIdsApartadosVencidos(any(),any())).thenReturn(List.of(18L));
        servicio.prorrogar(7L,30L,OffsetDateTime.now(reloj).plusHours(1));servicio.liberarCitasVencidas();
        assertThat(a.getEstado()).isEqualTo("PENDIENTE_PAGO");
        servicio.prorrogar(7L,30L,OffsetDateTime.now(reloj).minusSeconds(1));servicio.liberarCitasVencidas();
        assertThat(a.getEstado()).isEqualTo("LIBERADA_SIN_PAGO");
    }
    @Test void bloqueaPrestadorAntesDeCitasAlConfirmar() {
        servicio.confirmar(7L,30L);
        var locks=inOrder(prestadores,citas,em);
        locks.verify(prestadores).bloquearPorEmpresa(7L,8L);
        locks.verify(citas).findByEmpresaIdAndReservaGrupoIdOrderByIdAsc(7L,"grupo");
        locks.verify(em).refresh(a,LockModeType.PESSIMISTIC_WRITE);
    }
    private static CitaEntidad miembro(Long id) {
        var cita=new CitaEntidad();cita.setId(id);cita.setEmpresaId(7L);cita.setClienteId(44L);cita.setSucursalId(3L);cita.setPrestadorId(8L);
        cita.setReservaGrupoId("grupo");cita.setEstado("PENDIENTE_PAGO");
        cita.setInicio(LocalDateTime.of(2026,12,1,10,id==18L?0:30));cita.setFin(cita.getInicio().plusMinutes(30));return cita;
    }
}
