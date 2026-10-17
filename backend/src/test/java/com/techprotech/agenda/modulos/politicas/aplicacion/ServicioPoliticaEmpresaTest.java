package com.techprotech.agenda.modulos.politicas.aplicacion;

import com.techprotech.agenda.modulos.politicas.api.dto.*;
import com.techprotech.agenda.modulos.politicas.dominio.ModoNoShow;
import com.techprotech.agenda.modulos.politicas.infraestructura.entidad.PoliticaEmpresaEntidad;
import com.techprotech.agenda.modulos.politicas.infraestructura.repositorio.PoliticaEmpresaRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.EmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.admin.infraestructura.repositorio.AuditoriaConfiguracionEmpresaRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioConfigEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class ServicioPoliticaEmpresaTest {
    final PoliticaEmpresaRepositorio politicas = mock(PoliticaEmpresaRepositorio.class);
    final EmpresaRepositorio empresas = mock(EmpresaRepositorio.class);
    final EmpresaSitioConfigRepositorio sitios = mock(EmpresaSitioConfigRepositorio.class);
    final AuditoriaConfiguracionEmpresaRepositorio auditoria = mock(AuditoriaConfiguracionEmpresaRepositorio.class);
    final AtomicReference<PoliticaEmpresaEntidad> vigente = new AtomicReference<>();
    final UsuarioAutenticado actor = new UsuarioAutenticado("admin@example.com", 9L, 7L, "EMPRESA", List.of(), List.of(), List.of());
    ServicioPoliticaEmpresa servicio;
    @BeforeEach void configurar() {
        var defaults = new PredeterminadosPolitica(); defaults.setModoNoShow(ModoNoShow.CREDITO_REAGENDAR);
        defaults.setDiasReagendar(45); defaults.setDiasAclaracion(100); defaults.setReagendadosMax(2);
        defaults.setRecordatorio1Dias(9); defaults.setRecordatorio2Dias(3);
        servicio = new ServicioPoliticaEmpresa(politicas, empresas, sitios, auditoria, defaults,
                Clock.fixed(Instant.parse("2026-10-17T16:15:00Z"), ZoneId.of("America/Mexico_City")), mock(com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio.class), mock(com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.EmpresaCheckoutConfigRepositorio.class));
        var sitio = new EmpresaSitioConfigEntidad(); sitio.setEmpresaId(7L); sitio.setSlug("negocio");
        sitio.setPublicado(true); sitio.setNombreComercial("Negocio de prueba"); sitio.setCorreo("negocio@example.com");
        when(sitios.findById(7L)).thenReturn(Optional.of(sitio)); when(sitios.findBySlug("negocio")).thenReturn(Optional.of(sitio));
        when(empresas.bloquearPorId(7L)).thenReturn(Optional.of(new EmpresaEntidad()));
        when(politicas.findFirstByEmpresaIdOrderByVersionDesc(7L)).thenAnswer(i -> Optional.ofNullable(vigente.get()));
        when(politicas.saveAndFlush(any())).thenAnswer(i -> {var p = (PoliticaEmpresaEntidad)i.getArgument(0); p.setId((long)p.getVersion()); vigente.set(p); return p;});
    }
    PublicarPoliticaRequest solicitud(int version, ModoNoShow modo, int dias, int recordatorio) {
        return new PublicarPoliticaRequest(version, modo, dias, 100, 2, recordatorio, 3,
                "Texto <b>literal</b>", "negocio@example.com", null);
    }
    @Test void unDiaUsaSingularSinCambiarPluralesExistentes() {
        var r = new PublicarPoliticaRequest(0, ModoNoShow.RETENER, 1, 1, 1, 7, 2, null, "n@example.com", null);
        assertThat(servicio.vistaPrevia(r)).contains("1 día natural").doesNotContain("1 días");
        var credito = solicitud(0, ModoNoShow.CREDITO_REAGENDAR,45,9);
        assertThat(servicio.vistaPrevia(credito)).contains("45 días naturales", "100 días naturales");
        // El texto singular de crédito también se genera, sin relajar validaciones de recordatorios.
        assertThat(org.springframework.test.util.ReflectionTestUtils.invokeMethod(servicio,"texto",new PublicarPoliticaRequest(0,ModoNoShow.CREDITO_REAGENDAR,1,1,1,7,2,null,"n@example.com",null)).toString()).contains("1 día natural").doesNotContain("1 días");
    }
    @Test void alertaCalculadaPorTenantSinPublicarPolitica() {
        var repo = (com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio) org.springframework.test.util.ReflectionTestUtils.getField(servicio,"servicios");
        when(repo.existsByEmpresaIdAndActivoTrueAndRequiereAnticipoTrue(7L)).thenReturn(true);
        assertThat(servicio.obtener(7L).avisoCobro()).contains("no pueden reservarse");
        servicio.publicar(actor,solicitud(0,ModoNoShow.RETENER,45,9));
        assertThat(servicio.obtener(7L).avisoCobro()).isNull();
    }
    @Test void borradorUsaConfiguracionBackendSinPublicarNiActivarCobros() {
        var r = servicio.obtener(7L); assertThat(r.vigente()).isNull();
        assertThat(r.borrador().diasReagendar()).isEqualTo(45); assertThat(r.borrador().reagendadosMax()).isEqualTo(2);
        assertThatThrownBy(() -> servicio.exigirPublicada(7L)).isInstanceOf(ResponseStatusException.class);
        verify(politicas, never()).saveAndFlush(any());
    }
    @Test void publicaVersionInmutableConUtcAuditoriaYLockEmpresa() {
        var r = servicio.publicar(actor, solicitud(0, ModoNoShow.CREDITO_REAGENDAR, 45, 9));
        assertThat(r.vigente().version()).isEqualTo(1); assertThat(r.vigente().vigenteDesde().toString()).isEqualTo("2026-10-17T16:15Z");
        assertThat(vigente.get().getTextoSha256()).hasSize(64); assertThat(r.vigente().textoGenerado()).contains("45 días", "100 días");
        var orden = inOrder(empresas, politicas, auditoria); orden.verify(empresas).bloquearPorId(7L);
        orden.verify(politicas).findFirstByEmpresaIdOrderByVersionDesc(7L); orden.verify(politicas).saveAndFlush(any()); orden.verify(auditoria).save(any());
    }
    @Test void cambioCreaOtraVersionSinEditarAnteriorYRepeticionNoDuplica() {
        servicio.publicar(actor, solicitud(0, ModoNoShow.CREDITO_REAGENDAR, 45, 9)); var original = vigente.get();
        servicio.publicar(actor, solicitud(1, ModoNoShow.CREDITO_REAGENDAR, 45, 9)); verify(politicas, times(1)).saveAndFlush(any());
        servicio.publicar(actor, solicitud(1, ModoNoShow.CREDITO_REAGENDAR, 50, 9));
        assertThat(original.getDiasReagendar()).isEqualTo(45); assertThat(vigente.get().getVersion()).isEqualTo(2);
        verify(politicas, never()).save(any());
    }
    @Test void cambioSoloEnAvisosTambienVersiona() {
        servicio.publicar(actor, solicitud(0, ModoNoShow.CREDITO_REAGENDAR, 45, 9));
        servicio.publicar(actor, solicitud(1, ModoNoShow.CREDITO_REAGENDAR, 45, 8)); assertThat(vigente.get().getVersion()).isEqualTo(2);
    }
    @Test void versionBaseAntiguaRechazaSinPublicar() {
        servicio.publicar(actor, solicitud(0, ModoNoShow.CREDITO_REAGENDAR, 45, 9));
        assertThatThrownBy(() -> servicio.publicar(actor, solicitud(0, ModoNoShow.RETENER, 45, 9)))
                .isInstanceOf(ResponseStatusException.class).extracting(e -> ((ResponseStatusException)e).getStatusCode().value()).isEqualTo(409);
        verify(politicas, times(1)).saveAndFlush(any());
    }
    @Test void retenerConservaAclaracionPeroNoPlazosDeCredito() {
        var r = servicio.publicar(actor, solicitud(0, ModoNoShow.RETENER, 45, 9));
        assertThat(r.vigente().diasAclaracion()).isEqualTo(100); assertThat(vigente.get().getDiasReagendar()).isNull();
        assertThat(vigente.get().getRecordatorio1Dias()).isNull(); assertThat(r.vigente().textoGenerado()).contains("se retiene");
    }
    @Test void plazosIncoherentesNoSePublican() {
        assertThatThrownBy(() -> servicio.publicar(actor, solicitud(0, ModoNoShow.CREDITO_REAGENDAR, 101, 9))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> servicio.publicar(actor, solicitud(0, ModoNoShow.CREDITO_REAGENDAR, 45, 45))).isInstanceOf(ResponseStatusException.class);
        verify(politicas, never()).saveAndFlush(any());
    }
    @Test void faltaContactoNoPublica() {
        var r = new PublicarPoliticaRequest(0, ModoNoShow.RETENER, null, 90, null, null, null, null, " ", null);
        assertThatThrownBy(() -> servicio.publicar(actor, r)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(empresas, auditoria);
    }
    @Test void publicoTenantAjenoSinPoliticaOSitioNoPublicadoDaMismo404() {
        assertThatThrownBy(() -> servicio.publica("otro")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        assertThatThrownBy(() -> servicio.publica("negocio")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        servicio.publicar(actor, solicitud(0, ModoNoShow.RETENER, 45, 9));
        sitios.findById(7L).orElseThrow().setPublicado(false);
        assertThatThrownBy(() -> servicio.publica("negocio")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        verify(politicas, never()).findFirstByEmpresaIdOrderByVersionDesc(8L);
    }
    @Test void plataformaNoPublicaComoEmpresa() {
        var plataforma = new UsuarioAutenticado("p@example.com", 9L, null, "PLATAFORMA", List.of(), List.of(), List.of());
        assertThatThrownBy(() -> servicio.publicar(plataforma, solicitud(0, ModoNoShow.RETENER, 45, 9))).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(empresas, auditoria);
    }
}
