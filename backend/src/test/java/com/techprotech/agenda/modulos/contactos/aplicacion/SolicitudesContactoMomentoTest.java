package com.techprotech.agenda.modulos.contactos.aplicacion;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.EmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.contactos.infraestructura.entidad.SolicitudContactoEntidad;
import com.techprotech.agenda.modulos.contactos.infraestructura.repositorio.SolicitudContactoRepositorio;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Isolated("Momentos UTC de contactos con JVM México")
class SolicitudesContactoMomentoTest {
    @ParameterizedTest @ValueSource(strings={"America/Mexico_City","Asia/Tokyo"})
    void fechaDeNotificacionYCreacionNoSeReinterpretanEnZonaDeEmpresa(String zonaEmpresa) {
        var anterior=TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Mexico_City"));
            var empresas=mock(EmpresaRepositorio.class);var solicitudes=mock(SolicitudContactoRepositorio.class);
            var empresa=new EmpresaEntidad();empresa.setId(7L);empresa.setZonaHoraria(zonaEmpresa);
            when(empresas.findById(7L)).thenReturn(Optional.of(empresa));
            var solicitud=new SolicitudContactoEntidad();solicitud.setId(9L);solicitud.setEmpresaId(7L);
            solicitud.setCreadaEn(LocalDateTime.parse("2026-10-15T13:00"));
            when(solicitudes.findById(9L)).thenReturn(Optional.of(solicitud));
            when(solicitudes.findByEmpresaIdOrderByCreadaEnDesc(7L)).thenReturn(List.of(solicitud));
            var reloj=Clock.fixed(Instant.parse("2026-10-15T14:00:00Z"),ZoneOffset.UTC);
            var servicio=new ServicioGestionSolicitudesContacto(solicitudes,empresas,reloj);
            servicio.marcarNotificada(9L);
            var respuesta=servicio.listarPorEmpresa(7L).getFirst();
            assertThat(solicitud.getNotificadaEn()).isEqualTo(LocalDateTime.parse("2026-10-15T14:00"));
            assertThat(respuesta.notificadaEn()).isEqualTo(OffsetDateTime.parse("2026-10-15T14:00Z"));
            assertThat(respuesta.notificadaEn().getOffset()).isEqualTo(ZoneOffset.UTC);
            assertThat(respuesta.creadaEn()).isEqualTo(OffsetDateTime.parse("2026-10-15T13:00Z"));
            assertThat(respuesta.creadaEn().getOffset()).isEqualTo(ZoneOffset.UTC);
            assertThat(respuesta.empresaId()).isEqualTo(7L);
            verify(solicitudes).findByEmpresaIdOrderByCreadaEnDesc(7L);
        } finally {TimeZone.setDefault(anterior);}
    }

    @Test void conservaMomentosNulosSinInventarFecha() {
        var empresas=mock(EmpresaRepositorio.class);var solicitudes=mock(SolicitudContactoRepositorio.class);
        when(empresas.findById(7L)).thenReturn(Optional.of(new EmpresaEntidad()));
        var solicitud=new SolicitudContactoEntidad();solicitud.setEmpresaId(7L);
        when(solicitudes.findByEmpresaIdOrderByCreadaEnDesc(7L)).thenReturn(List.of(solicitud));
        var servicio=new ServicioGestionSolicitudesContacto(solicitudes,empresas,Clock.fixed(Instant.EPOCH,ZoneOffset.UTC));
        var respuesta=servicio.listarPorEmpresa(7L).getFirst();
        assertThat(respuesta.creadaEn()).isNull();assertThat(respuesta.notificadaEn()).isNull();
    }
}
