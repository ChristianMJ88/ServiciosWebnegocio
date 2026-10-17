package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.admin.infraestructura.repositorio.AuditoriaConfiguracionEmpresaRepositorio;
import com.techprotech.agenda.modulos.caja.aplicacion.ServicioVentasCheckout;
import com.techprotech.agenda.modulos.caja.infraestructura.entidad.VentaEntidad;
import com.techprotech.agenda.modulos.caja.infraestructura.repositorio.VentaRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.disponibilidad.aplicacion.*;
import com.techprotech.agenda.modulos.pagos.api.dto.ResolverIncidenciaRequest;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenRepositorio;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServicioResolucionIncidenciasCheckoutTest {
    final CheckoutOrdenRepositorio ordenes=mock(CheckoutOrdenRepositorio.class);
    final ServicioConfirmacionCitasCheckout citas=mock(ServicioConfirmacionCitasCheckout.class);
    final ServicioInventarioCheckout inventario=mock(ServicioInventarioCheckout.class);
    final ServicioConsultaDisponibilidad disponibilidad=mock(ServicioConsultaDisponibilidad.class);
    final ServicioVentasCheckout proyeccion=mock(ServicioVentasCheckout.class);
    final VentaRepositorio ventas=mock(VentaRepositorio.class);
    final AuditoriaConfiguracionEmpresaRepositorio auditoria=mock(AuditoriaConfiguracionEmpresaRepositorio.class);
    final EntityManager em=mock(EntityManager.class);
    final ServicioResolucionIncidenciasCheckout servicio=new ServicioResolucionIncidenciasCheckout(ordenes,citas,inventario,disponibilidad,proyeccion,ventas,auditoria,em,new ObjectMapper().findAndRegisterModules());
    final UsuarioAutenticado usuario=new UsuarioAutenticado("admin@test.local",9L,7L,"EMPRESA",List.of(),List.of(),List.of(3L));
    final CheckoutOrdenEntidad orden=new CheckoutOrdenEntidad();
    @BeforeEach void preparar() {
        orden.setId(30L);orden.setEmpresaId(7L);orden.setSucursalId(3L);orden.setEstado("PAGADA_CON_INCIDENCIA");orden.setRequiereConciliacion(true);
        when(ordenes.findByEmpresaIdAndPublicId(7L,"publica")).thenReturn(Optional.of(orden));
        when(ordenes.bloquearPorEmpresaEId(7L,30L)).thenReturn(Optional.of(orden));
    }
    @Test void resuelveOtraConActorMotivoYAuditoriaSinInventarConfirmacion() {
        servicio.resolver(usuario,"publica",new ResolverIncidenciaRequest(ResolverIncidenciaRequest.Tipo.OTRA,"Entrega coordinada con cliente",List.of()));
        assertThat(orden.isRequiereConciliacion()).isFalse();
        verify(proyeccion).proyectar(7L,30L);
        verify(auditoria).save(argThat(a -> a.getEmpresaId()==7L && a.getUsuarioActorId()==9L && a.getDetalleDespuesJson().contains("Entrega coordinada")));
        verify(citas,never()).confirmar(any(),any(),any(),any());
    }
    @Test void noResuelveDosVecesNiOtroTenant() {
        orden.setRequiereConciliacion(false);
        assertThatThrownBy(() -> servicio.resolver(usuario,"publica",new ResolverIncidenciaRequest(ResolverIncidenciaRequest.Tipo.OTRA,"Revisión",List.of()))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> servicio.resolver(usuario,"ajena",new ResolverIncidenciaRequest(ResolverIncidenciaRequest.Tipo.OTRA,"Revisión",List.of()))).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(auditoria,proyeccion);
    }
    @Test void impideResolverFueraDelScopeSucursal() {
        orden.setSucursalId(4L);
        assertThatThrownBy(() -> servicio.resolver(usuario,"publica",new ResolverIncidenciaRequest(ResolverIncidenciaRequest.Tipo.OTRA,"Revisión",List.of()))).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(auditoria,proyeccion);
    }
    @Test void reembolsoDeclaradoFueraDelSistemaRetiraCreditoDeVentaSinEnviarDinero() {
        var venta=new VentaEntidad();venta.setEstado("PAGADA");
        when(ventas.findByEmpresaIdAndCheckoutOrdenId(7L,30L)).thenReturn(Optional.of(venta));
        servicio.resolver(usuario,"publica",new ResolverIncidenciaRequest(ResolverIncidenciaRequest.Tipo.REEMBOLSADA_FUERA_DEL_SISTEMA,"Referencia bancaria comprobada",List.of()));
        assertThat(orden.getEstado()).isEqualTo("REEMBOLSADA");assertThat(venta.getEstado()).isEqualTo("REEMBOLSADA");
        verifyNoInteractions(inventario);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"LIBERADA_SIN_PAGO","PENDIENTE"})
    void reprogramaYConfirmaConPagoExistenteDespuesDeValidarYGuardarHorarios(String estado) {
        var cita=new CitaEntidad();cita.setId(10L);cita.setSucursalId(3L);cita.setServicioId(4L);cita.setPrestadorId(5L);cita.setEstado(estado);cita.setPoliticaCobro("SIN_COBRO");
        var inicio=LocalDateTime.of(2026,12,1,10,0);cita.setInicio(inicio.minusDays(1));cita.setFin(cita.getInicio().plusMinutes(30));
        when(citas.bloquearGrupoOrden(7L,30L)).thenReturn(List.of(cita));
        var zona=ZoneId.of("America/Mexico_City");
        when(disponibilidad.obtenerFranjasDisponibles(7L,3L,4L,5L,inicio.toLocalDate())).thenReturn(List.of(new FranjaDisponibleResponse(
                inicio.atZone(zona).toOffsetDateTime().toString(),
                inicio.plusMinutes(30).atZone(zona).toOffsetDateTime().toString(),5L,4L,3L)));
        when(inventario.confirmarSiDisponible(7L,30L)).thenReturn(true);when(citas.confirmar(7L,30L,com.techprotech.agenda.modulos.citas.dominio.eventos.OrigenEventoCita.ADMIN,9L)).thenReturn(true);
        servicio.resolver(usuario,"publica",new ResolverIncidenciaRequest(ResolverIncidenciaRequest.Tipo.REPROGRAMADA,"Nueva fecha acordada",List.of(new ResolverIncidenciaRequest.Reprogramacion(10L,inicio))));
        assertThat(cita.getInicio()).isEqualTo(inicio);assertThat(orden.getEstado()).isEqualTo("PAGADA");
        var secuencia=inOrder(em,citas);secuencia.verify(em,times(2)).flush();secuencia.verify(citas).confirmar(7L,30L,com.techprotech.agenda.modulos.citas.dominio.eventos.OrigenEventoCita.ADMIN,9L);
        verify(auditoria).save(any());
    }
    @Test void horarioOcupadoNoCierraIncidenciaNiConfirma() {
        var cita=new CitaEntidad();cita.setId(10L);cita.setSucursalId(3L);cita.setServicioId(4L);cita.setPrestadorId(5L);cita.setEstado("LIBERADA_SIN_PAGO");
        var inicio=LocalDateTime.of(2026,12,1,10,0);cita.setInicio(inicio);cita.setFin(inicio.plusMinutes(30));
        when(citas.bloquearGrupoOrden(7L,30L)).thenReturn(List.of(cita));
        assertThatThrownBy(() -> servicio.resolver(usuario,"publica",new ResolverIncidenciaRequest(ResolverIncidenciaRequest.Tipo.REPROGRAMADA,"Nueva fecha",List.of(new ResolverIncidenciaRequest.Reprogramacion(10L,inicio.plusDays(1)))))).isInstanceOf(ResponseStatusException.class);
        assertThat(orden.isRequiereConciliacion()).isTrue();verify(citas,never()).confirmar(any(),any(),any(),any());verifyNoInteractions(auditoria);
    }
}
