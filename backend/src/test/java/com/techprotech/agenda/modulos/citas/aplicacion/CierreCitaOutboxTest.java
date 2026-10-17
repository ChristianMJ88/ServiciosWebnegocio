package com.techprotech.agenda.modulos.citas.aplicacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.compartido.correo.BandejaSalidaNotificacionRepositorio;
import com.techprotech.agenda.compartido.whatsapp.ClienteWhatsappTwilio;
import com.techprotech.agenda.compartido.whatsapp.ServicioOutboxWhatsappCitas;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class CierreCitaOutboxTest {
    @Test void agradecimientoWhatsappUsaLaMismaDeduplicacionQueCorreoSinEnviarAlProveedor() {
        var bandeja=mock(BandejaSalidaNotificacionRepositorio.class);var proveedor=mock(ClienteWhatsappTwilio.class);
        var servicio=new ServicioOutboxWhatsappCitas(bandeja,proveedor,new ObjectMapper().findAndRegisterModules(),Clock.fixed(Instant.parse("2026-10-15T16:00:00Z"),ZoneOffset.UTC));
        when(bandeja.existsByAgregadoIdAndCanalAndTipoEventoAndEstadoIn(1L,"WHATSAPP","CITA_GRACIAS_VISITA_WHATSAPP",List.of("PENDIENTE","PROCESANDO","ENVIADA")))
                .thenReturn(false,true);
        when(proveedor.estaHabilitado(7L)).thenReturn(true);
        servicio.programarGraciasVisita(7L,1L,"5555555555",LocalDateTime.parse("2026-10-15T10:00"));
        servicio.programarGraciasVisita(7L,1L,"5555555555",LocalDateTime.parse("2026-10-15T10:00"));
        verify(bandeja,times(1)).save(any());verify(proveedor,times(1)).estaHabilitado(7L);verifyNoMoreInteractions(proveedor);
    }
}
