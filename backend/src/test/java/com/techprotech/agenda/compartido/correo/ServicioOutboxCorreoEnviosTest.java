package com.techprotech.agenda.compartido.correo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ServicioOutboxCorreoEnviosTest {
    @Test
    void programaUnSoloAvisoPorHitoDelEnvio() {
        BandejaSalidaNotificacionRepositorio repositorio = mock(BandejaSalidaNotificacionRepositorio.class);
        ServicioConfiguracionCorreoEmpresa configuracion = mock(ServicioConfiguracionCorreoEmpresa.class);
        ConfiguracionCorreoResolvida activa = new ConfiguracionCorreoResolvida(
                25L, true, ProveedorCorreo.SMTP, "envios@example.com", "Negocio", null,
                "localhost", 1025, null, null, false, false,
                null, null, null, null, null, null, null);
        when(configuracion.resolver(25L)).thenReturn(activa);
        ServicioOutboxCorreoEnvios servicio = new ServicioOutboxCorreoEnvios(
                repositorio, configuracion, new ObjectMapper(), java.time.Clock.systemUTC());

        assertThat(servicio.programar(25L, 9L, "ENTREGADO")).isTrue();
        ArgumentCaptor<BandejaSalidaNotificacionEntidad> captor =
                ArgumentCaptor.forClass(BandejaSalidaNotificacionEntidad.class);
        verify(repositorio).save(captor.capture());
        assertThat(captor.getValue().getTipoAgregado()).isEqualTo("ENVIO");
        assertThat(captor.getValue().getTipoEvento()).isEqualTo("ENVIO_ENTREGADO_EMAIL");

        when(repositorio.existsByAgregadoIdAndCanalAndTipoEventoAndEstadoIn(
                eq(9L), eq("EMAIL"), eq("ENVIO_ENTREGADO_EMAIL"), any())).thenReturn(true);
        assertThat(servicio.programar(25L, 9L, "ENTREGADO")).isTrue();
        verify(repositorio, times(1)).save(any());
    }
}
