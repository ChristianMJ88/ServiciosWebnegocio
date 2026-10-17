package com.techprotech.agenda.modulos.whatsapp.aplicacion;

import com.techprotech.agenda.compartido.whatsapp.ContextoEmpresaWhatsapp;
import com.techprotech.agenda.modulos.citas.api.dto.CitaClienteResponse;
import com.techprotech.agenda.modulos.citas.aplicacion.ServicioCitasCliente;
import com.techprotech.agenda.modulos.clientes.infraestructura.entidad.ClienteEntidad;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CitasBotTiempoTest {
    @Test void proximasIncluyeUnaCitaDentroDeDosHorasUsandoSuOffsetReal() {
        var servicio = mock(ServicioWhatsappCitas.class, CALLS_REAL_METHODS);
        var citas = mock(ServicioCitasCliente.class);
        var contexto = new ContextoEmpresaWhatsapp();
        ReflectionTestUtils.setField(servicio,"servicioCitasCliente",citas);
        ReflectionTestUtils.setField(servicio,"contextoEmpresaWhatsapp",contexto);
        ReflectionTestUtils.setField(servicio,"reloj",Clock.fixed(Instant.parse("2026-10-15T14:00:00Z"),ZoneOffset.UTC));
        var cliente = new ClienteEntidad(); cliente.setUsuarioId(11L);
        var proxima = cita(1L,"2026-10-15T10:00:00-06:00");
        var pasada = cita(2L,"2026-10-15T07:59:00-06:00");
        var otroOffset = cita(3L,"2026-10-15T15:30:00+01:00");
        when(citas.listarMisCitas(7L,11L)).thenReturn(List.of(proxima,pasada,otroOffset));
        List<CitaClienteResponse> resultado = contexto.ejecutar(7L,
                () -> ReflectionTestUtils.invokeMethod(servicio,"obtenerCitasActivasCliente",cliente));
        assertThat(resultado).extracting(CitaClienteResponse::id).containsExactly(3L,1L);
        verify(citas).listarMisCitas(7L,11L);
    }

    @Test void folioSoloResuelveCitasPropiasDelTelefonoYTenant() {
        var servicio = mock(ServicioWhatsappCitas.class, CALLS_REAL_METHODS);
        var citas = mock(ServicioCitasCliente.class); var contexto = new ContextoEmpresaWhatsapp();
        ReflectionTestUtils.setField(servicio,"servicioCitasCliente",citas);
        ReflectionTestUtils.setField(servicio,"contextoEmpresaWhatsapp",contexto);
        ReflectionTestUtils.setField(servicio,"reloj",Clock.fixed(Instant.parse("2026-10-15T14:00:00Z"),ZoneOffset.UTC));
        var cliente = new ClienteEntidad(); cliente.setUsuarioId(11L);
        when(citas.listarMisCitas(7L,11L)).thenReturn(List.of(cita(1L,"2026-10-15T10:00:00-06:00")));
        Long id = contexto.ejecutar(7L,() -> ReflectionTestUtils.invokeMethod(servicio,"resolverCitaPorOpcion",cliente,"fl-abcdefg1",false));
        assertThat(id).isEqualTo(1L);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> contexto.ejecutar(7L,() -> ReflectionTestUtils.invokeMethod(servicio,"resolverCitaPorOpcion",cliente,"FL-AJENO999",false)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("No encontré esa opción");
        verify(citas,times(2)).listarMisCitas(7L,11L);
    }
    private CitaClienteResponse cita(Long id, String inicio) {
        var fecha = OffsetDateTime.parse(inicio);
        return new CitaClienteResponse(id,"CONFIRMADA",3L,8L,9L,null,null,null,fecha,fecha.plusHours(1),
                null,"MXN",null,true,null,null,null, "FL-ABCDEFG" + id);
    }
}
