package com.techprotech.agenda.modulos.plataforma.aplicacion;

import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.SuscripcionPlataformaEntidad;
import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.SuscripcionPlataformaRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProcesadorVencimientosSuscripcionTest {
    private final SuscripcionPlataformaRepositorio suscripciones=mock(SuscripcionPlataformaRepositorio.class);
    private final ServicioAvisosSuscripcion avisos=mock(ServicioAvisosSuscripcion.class);
    private final ProcesadorVencimientosSuscripcion procesador=new ProcesadorVencimientosSuscripcion(suscripciones,avisos,25, java.time.Clock.systemUTC());

    @Test void registraElAvisoSinCambiarLaPoliticaDeAcceso() {
        var s=new SuscripcionPlataformaEntidad();s.setEstado("GRACIA");s.setEmpresaId(3L);
        when(suscripciones.findVencidasSinAvisar(any(),eq(PageRequest.of(0,25)))).thenReturn(List.of(s));
        procesador.procesar();
        verify(avisos).programar(eq(s),eq("VENCIMIENTO"),anyString(),isNull());
        assertThat(s.getAvisoVencimientoEn()).isNotNull();
        assertThat(s.getEstado()).isEqualTo("GRACIA");
        assertThat(s.getEmpresaId()).isEqualTo(3L);
        verify(suscripciones).save(s);
    }

    @Test void falloAlProgramarPermiteReintentarSinMarcarComoNotificado() {
        var s=new SuscripcionPlataformaEntidad();s.setEstado("PRUEBA");
        when(suscripciones.findVencidasSinAvisar(any(),any())).thenReturn(List.of(s));
        doThrow(new IllegalStateException("outbox no disponible")).when(avisos).programar(eq(s),anyString(),anyString(),isNull());
        assertThatThrownBy(procesador::procesar).isInstanceOf(IllegalStateException.class);
        assertThat(s.getAvisoVencimientoEn()).isNull();
        verify(suscripciones,never()).save(any());
    }
}
