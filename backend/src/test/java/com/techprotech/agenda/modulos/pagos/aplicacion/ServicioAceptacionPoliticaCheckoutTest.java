package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.modulos.pagos.api.dto.CrearOrdenCheckoutRequest;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenDetalleRepositorio;
import com.techprotech.agenda.modulos.politicas.aplicacion.ServicioPoliticaEmpresa;
import com.techprotech.agenda.modulos.politicas.infraestructura.entidad.PoliticaEmpresaEntidad;
import com.techprotech.agenda.modulos.politicas.infraestructura.repositorio.PoliticaEmpresaRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class ServicioAceptacionPoliticaCheckoutTest {
    final ServicioPoliticaEmpresa politicas = mock(ServicioPoliticaEmpresa.class);
    final PoliticaEmpresaRepositorio versiones = mock(PoliticaEmpresaRepositorio.class);
    final CheckoutOrdenDetalleRepositorio detalles = mock(CheckoutOrdenDetalleRepositorio.class);
    final ServicioConfirmacionCitasCheckout confirmacion = mock(ServicioConfirmacionCitasCheckout.class);
    final com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio citas = mock(com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio.class);
    final ServicioAceptacionPoliticaCheckout servicio = new ServicioAceptacionPoliticaCheckout(politicas, versiones, detalles,
            confirmacion, Clock.fixed(Instant.parse("2026-10-17T18:00:00Z"), ZoneId.of("America/Mexico_City")), citas);
    final CheckoutOrdenEntidad orden = new CheckoutOrdenEntidad();
    @BeforeEach void preparar() {
        when(citas.acreditarPoliticaSiAusente(any(),any(),any(),any())).thenReturn(1);
        orden.setEmpresaId(7L); orden.setId(50L);
        var p = new PoliticaEmpresaEntidad(); p.setId(91L); p.setEmpresaId(7L); p.setVersion(4);
        when(politicas.exigirPublicada(7L)).thenReturn(p); when(versiones.findByEmpresaIdAndId(7L, 91L)).thenReturn(Optional.of(p));
        var d = new CheckoutOrdenDetalleEntidad(); d.setTipoConcepto("SERVICIO"); d.setCitaId(3L);
        when(detalles.findByEmpresaIdAndCheckoutOrdenIdOrderByIdAsc(7L,50L)).thenReturn(List.of(d));
    }
    CrearOrdenCheckoutRequest request(Integer version, Boolean acepta) {
        return new CrearOrdenCheckoutRequest(2L,null,"c@example.com","Cliente","2220000000","RECOGE_EN_SUCURSAL",null,null,List.of(),List.of(3L),version,acepta);
    }
    @Test void serviciosExigenAceptacionExplicitaYVersionActual() {
        assertThatThrownBy(() -> servicio.aceptar(orden, 7L, request(4,false),true)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> servicio.aceptar(orden, 7L, request(3,true),true)).isInstanceOf(ResponseStatusException.class);
        assertThat(orden.getPoliticaVersionId()).isNull();
    }
    @Test void guardaVersionYMomentoUtcEnOrden() {
        servicio.aceptar(orden,7L,request(4,true),true);
        assertThat(orden.getPoliticaVersionId()).isEqualTo(91L); assertThat(orden.getPoliticaAceptadaEn()).isEqualTo("2026-10-17T18:00:00");
    }
    @Test void productosNoRequierenCasillaNiGuardanAceptacion() {
        servicio.aceptar(orden,7L,request(null,null),false);
        assertThat(orden.getPoliticaVersionId()).isNull(); assertThat(orden.getPoliticaAceptadaEn()).isNull();
        verify(politicas).exigirPublicada(7L);
    }
    @Test void reintentoNoPuedeCambiarVersionNiOmitirAceptacion() {
        servicio.aceptar(orden,7L,request(4,true),true); servicio.validarReintento(orden, request(4,true));
        assertThatThrownBy(() -> servicio.validarReintento(orden, request(null,null))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> servicio.validarReintento(orden, request(5,true))).isInstanceOf(ResponseStatusException.class);
    }
    @Test void ordenServicioLegadaSinAceptacionNoPuedeCrearOtroPago() {
        assertThatThrownBy(() -> servicio.validarAntesDePagar(orden)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void versionReemplazadaRequiereNuevaAceptacionAntesDelPago() {
        servicio.aceptar(orden,7L,request(4,true),true); servicio.validarAntesDePagar(orden);
        var nueva = new PoliticaEmpresaEntidad(); nueva.setId(92L); when(politicas.exigirPublicada(7L)).thenReturn(nueva);
        assertThatThrownBy(() -> servicio.validarAntesDePagar(orden)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void aprobacionInclusoConIncidenciaCopiaSoloEnCitasPropiasSinReescribirHistoria() {
        servicio.aceptar(orden,7L,request(4,true),true);
        var propia = new CitaEntidad(); propia.setId(3L); propia.setEmpresaId(7L);
        var ajena = new CitaEntidad(); ajena.setId(3L); ajena.setEmpresaId(8L);
        when(confirmacion.bloquearGrupoOrden(7L,50L)).thenReturn(List.of(propia,ajena));
        servicio.acreditarAceptacion(orden); assertThat(propia.getPoliticaVersionId()).isEqualTo(91L); assertThat(ajena.getPoliticaVersionId()).isNull();
        orden.setPoliticaVersionId(92L); servicio.acreditarAceptacion(orden);
        assertThat(propia.getPoliticaVersionId()).isEqualTo(91L);
    }
    @Test void pagoHistoricoAprobadoNoSeDescartaNiInventaAceptacion() {
        servicio.acreditarAceptacion(orden); verifyNoInteractions(confirmacion); assertThat(orden.getPoliticaVersionId()).isNull();
    }
    @Test void liquidarCitaLegadaNoImponePoliticaAlAnticipoPrevio() {
        servicio.aceptar(orden,7L,request(4,true),true);
        var cita = new CitaEntidad(); cita.setId(3L); cita.setEmpresaId(7L);
        when(confirmacion.bloquearGrupoOrden(7L,50L)).thenReturn(List.of(cita));
        when(citas.buscarCitasConPagoAnterior(eq(7L),any(),eq(50L))).thenReturn(List.of(3L));
        servicio.acreditarAceptacion(orden);
        var ordenBloqueos = inOrder(confirmacion,citas);
        ordenBloqueos.verify(confirmacion).bloquearGrupoOrden(7L,50L);
        ordenBloqueos.verify(citas).buscarCitasConPagoAnterior(eq(7L),any(),eq(50L));
        assertThat(cita.getPoliticaVersionId()).isNull(); assertThat(orden.getPoliticaVersionId()).isEqualTo(91L);
        verify(citas,never()).acreditarPoliticaSiAusente(any(),any(),any(),any());
    }
    @Test void ordenDeProductosConCitasSinCobroRepiteSinAceptarPolitica() {
        when(detalles.findByEmpresaIdAndCheckoutOrdenIdOrderByIdAsc(7L,50L)).thenReturn(List.of());
        servicio.validarReintento(orden, request(4,true));
        assertThat(orden.getPoliticaVersionId()).isNull();
    }

}
