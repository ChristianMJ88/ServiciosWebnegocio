package com.techprotech.agenda.modulos.envios.aplicacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.envios.infraestructura.entidad.EmpresaEnvioConfigEntidad;
import com.techprotech.agenda.modulos.envios.infraestructura.entidad.EventoWebhookEnvioEntidad;
import com.techprotech.agenda.modulos.envios.infraestructura.repositorio.EmpresaEnvioConfigRepositorio;
import com.techprotech.agenda.modulos.envios.infraestructura.repositorio.EventoWebhookEnvioRepositorio;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ServicioWebhookEnviaTest {

    @Test
    void persisteUnaEntregaValidaAntesDeProcesarla() throws Exception {
        EmpresaEnvioConfigRepositorio configuraciones = mock(EmpresaEnvioConfigRepositorio.class);
        EventoWebhookEnvioRepositorio eventos = mock(EventoWebhookEnvioRepositorio.class);
        EmpresaEnvioConfigEntidad config = new EmpresaEnvioConfigEntidad();
        config.setEmpresaId(25L); config.setProveedor("ENVIA"); config.setHabilitado(true);
        String token = "token-webhook-tenant";
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
        when(configuraciones.findByWebhookTokenHashAndProveedorAndHabilitadoTrue(hash, "ENVIA"))
                .thenReturn(Optional.of(config));

        ServicioWebhookEnvia servicio = new ServicioWebhookEnvia(configuraciones, eventos,
                mock(ResolutorCredencialEnvio.class), new ObjectMapper(), java.time.Clock.systemUTC());
        String payload = """
                {"type":"tracking.simple","created_at":"2026-09-30T20:00:00Z","data":{"tracking_number":"ABC123","status":"delivered"}}
                """;

        servicio.recibir(token, "delivery-1", "tracking.simple", null, null, payload);

        ArgumentCaptor<EventoWebhookEnvioEntidad> captor = ArgumentCaptor.forClass(EventoWebhookEnvioEntidad.class);
        verify(eventos).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getEmpresaId()).isEqualTo(25L);
        assertThat(captor.getValue().getEventoExternoId()).isEqualTo("delivery-1");
        assertThat(captor.getValue().getEstadoProcesamiento()).isEqualTo("RECIBIDO");
    }

    @Test
    void ignoraLaEntregaDuplicadaDelMismoTenant() throws Exception {
        EmpresaEnvioConfigRepositorio configuraciones = mock(EmpresaEnvioConfigRepositorio.class);
        EventoWebhookEnvioRepositorio eventos = mock(EventoWebhookEnvioRepositorio.class);
        EmpresaEnvioConfigEntidad config = new EmpresaEnvioConfigEntidad();
        config.setEmpresaId(25L); config.setProveedor("ENVIA"); config.setHabilitado(true);
        String token = "token-webhook-tenant";
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
        when(configuraciones.findByWebhookTokenHashAndProveedorAndHabilitadoTrue(hash, "ENVIA"))
                .thenReturn(Optional.of(config));
        when(eventos.existsByEmpresaIdAndProveedorAndEventoExternoId(25L, "ENVIA", "delivery-1"))
                .thenReturn(true);
        ServicioWebhookEnvia servicio = new ServicioWebhookEnvia(configuraciones, eventos,
                mock(ResolutorCredencialEnvio.class), new ObjectMapper(), java.time.Clock.systemUTC());

        servicio.recibir(token, "delivery-1", "tracking.simple", null, null,
                "{\"type\":\"tracking.simple\",\"data\":{}}");

        verify(eventos, never()).saveAndFlush(any());
    }
}
