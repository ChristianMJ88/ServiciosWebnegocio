package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.modulos.pagos.dominio.TipoComisionPlataforma;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.ReglaComisionPlataformaEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.ProveedorPagoRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.ReglaComisionPlataformaRepositorio;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServicioComisionPlataformaTest {
    @Test
    void devuelveCeroCuandoNoExisteUnaReglaActiva() {
        ReglaComisionPlataformaRepositorio reglas = mock(ReglaComisionPlataformaRepositorio.class);
        when(reglas.resolver(eq(2L), eq(7L), eq("MXN"), any())).thenReturn(List.of());
        ServicioComisionPlataforma servicio = new ServicioComisionPlataforma(reglas, mock(ProveedorPagoRepositorio.class));

        assertThat(servicio.calcular(7L, 2L, new BigDecimal("500.00"), "MXN")).isEqualByComparingTo("0.00");
    }

    @Test
    void calculaPorcentajeYRespetaElLimiteMaximoConfigurado() {
        ReglaComisionPlataformaRepositorio reglas = mock(ReglaComisionPlataformaRepositorio.class);
        ReglaComisionPlataformaEntidad regla = new ReglaComisionPlataformaEntidad();
        regla.setTipo(TipoComisionPlataforma.PORCENTAJE); regla.setValor(new BigDecimal("5.0000"));
        regla.setImporteMaximo(new BigDecimal("20.00")); regla.setActiva(true);
        when(reglas.resolver(eq(2L), eq(7L), eq("MXN"), any())).thenReturn(List.of(regla));
        ServicioComisionPlataforma servicio = new ServicioComisionPlataforma(reglas, mock(ProveedorPagoRepositorio.class));

        assertThat(servicio.calcular(7L, 2L, new BigDecimal("500.00"), "MXN")).isEqualByComparingTo("20.00");
    }

    @Test
    void calculaUnMontoFijoConfiguradoPorElBackend() {
        ReglaComisionPlataformaRepositorio reglas = mock(ReglaComisionPlataformaRepositorio.class);
        ReglaComisionPlataformaEntidad regla = new ReglaComisionPlataformaEntidad();
        regla.setTipo(TipoComisionPlataforma.MONTO_FIJO); regla.setValor(new BigDecimal("7.50")); regla.setActiva(true);
        when(reglas.resolver(eq(2L), eq(7L), eq("MXN"), any())).thenReturn(List.of(regla));
        ServicioComisionPlataforma servicio = new ServicioComisionPlataforma(reglas, mock(ProveedorPagoRepositorio.class));

        assertThat(servicio.calcular(7L, 2L, new BigDecimal("500.00"), "MXN")).isEqualByComparingTo("7.50");
    }
}
